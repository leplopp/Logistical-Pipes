package plopp.pipecraft.Network.pipes;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.logic.pipe.PipeTravel;
import plopp.pipecraft.logic.pipe.TravellingItem;

public class TravellingItemSyncPacket implements CustomPacketPayload {
    private final TravellingItemData data;

    public static final CustomPacketPayload.Type<TravellingItemSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pipecraft", "travelling_item_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TravellingItemSyncPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, TravellingItemSyncPacket pkt) {
            TravellingItemData d = pkt.data;
            buf.writeUUID(d.id);
            buf.writeBlockPos(d.currentPos);
            buf.writeBlockPos(d.lastPos);
            buf.writeByte(d.side.get3DDataValue());
            buf.writeFloat(d.progress);
            buf.writeFloat(d.speed);

            CompoundTag tag = new CompoundTag();
            if (!d.stack.isEmpty()) {
                var encodeResult = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, d.stack);
                if (encodeResult.result().isPresent()) {
                    tag = (CompoundTag) encodeResult.result().get();
                }
            }
            buf.writeNbt(tag);
        }

        @Override
        public TravellingItemSyncPacket decode(RegistryFriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            BlockPos currentPos = buf.readBlockPos();
            BlockPos lastPos = buf.readBlockPos();
            Direction side = Direction.from3DDataValue(buf.readByte());
            float progress = buf.readFloat();
            float speed = buf.readFloat();

            CompoundTag tag = buf.readNbt();
            ItemStack stack = ItemStack.EMPTY;
            if (tag != null) {
                var decodeResult = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag);
                if (decodeResult.result().isPresent()) {
                    stack = decodeResult.result().get();
                }
            }

            return new TravellingItemSyncPacket(new TravellingItemData(id, stack, currentPos, lastPos, side, progress, speed));
        }
    };

    public TravellingItemSyncPacket(TravellingItem item) {
        this.data = new TravellingItemData(item.id, item.stack, item.currentPos, item.lastPos, item.side, (float)item.progress, (float)item.speed);
    }

    private TravellingItemSyncPacket(TravellingItemData data) {
        this.data = data;
    }

    public static void handle(TravellingItemSyncPacket packet, IPayloadContext context) {
        Minecraft.getInstance().execute(() -> {
            TravellingItemData d = packet.data;
            
            Optional<TravellingItem> opt = PipeTravel.activeItems.stream()
                    .filter(i -> i.id.equals(d.id))
                    .findFirst();
            
            if (opt.isPresent()) {
                TravellingItem item = opt.get();
                item.stack = d.stack;
                item.currentPos = d.currentPos;
                item.lastPos = d.lastPos;
                item.side = d.side;
                item.progress = d.progress;
                item.speed = d.speed;
            } else {
                TravellingItem item = new TravellingItem(d.stack, d.lastPos, d.side, PipeConfig.defaultConfig(), null);
                item.id = d.id;
                item.currentPos = d.currentPos;
                item.progress = d.progress;
                item.speed = d.speed;
                PipeTravel.activeItems.add(item);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private record TravellingItemData(UUID id, ItemStack stack, BlockPos currentPos, BlockPos lastPos, Direction side, float progress, float speed) {}
}