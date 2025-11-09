package plopp.pipecraft.Network.linker;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerScreen;

public class ViaductLinkerListPacket implements CustomPacketPayload {
    private List<DataEntryRecord> linkers;

    public static final CustomPacketPayload.Type<ViaductLinkerListPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "viaduct_linker_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ViaductLinkerListPacket> CODEC = new StreamCodec<>() {
    	@Override
    	public void encode(RegistryFriendlyByteBuf buf, ViaductLinkerListPacket pkt) {
    	    buf.writeInt(pkt.linkers.size());
    	    for (DataEntryRecord entry : pkt.linkers) {
    	        buf.writeBlockPos(entry.pos());
    	        buf.writeUtf(entry.name());

    	        CompoundTag tag = new CompoundTag();
    	        var encodeResult = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, entry.icon());
    	        if (encodeResult.result().isPresent()) {
    	            tag = (CompoundTag) encodeResult.result().get();
    	        }
    	        buf.writeNbt(tag);
    	    }
    	}

    	@Override
    	public ViaductLinkerListPacket decode(RegistryFriendlyByteBuf buf) {
    	    int size = buf.readInt();
    	    List<DataEntryRecord> linkers = new ArrayList<>();
    	    for (int i = 0; i < size; i++) {
    	        BlockPos pos = buf.readBlockPos();
    	        String name = buf.readUtf(32767);

    	        CompoundTag tag = buf.readNbt();
    	        ItemStack icon = ItemStack.EMPTY;
    	        if (tag != null) {
    	            var decodeResult = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag);
    	            if (decodeResult.result().isPresent()) {
    	                icon = decodeResult.result().get();
    	            }
    	        }
    	        linkers.add(new DataEntryRecord(pos, name, icon));
    	    }
    	    return new ViaductLinkerListPacket(linkers);
    	}
    };

    public ViaductLinkerListPacket(List<DataEntryRecord> linkers) {
        this.linkers = linkers;
    }

    public static void handle(ViaductLinkerListPacket packet, IPayloadContext context) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof ViaductLinkerScreen screen) {
                screen.getMenu().setLinkers(packet.linkers);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}