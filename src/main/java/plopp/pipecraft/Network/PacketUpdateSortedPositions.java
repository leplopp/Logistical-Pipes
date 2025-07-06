package plopp.pipecraft.Network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;

public class PacketUpdateSortedPositions implements CustomPacketPayload {
    private final BlockPos targetPos;
    private final List<BlockPos> positions;

    public static final CustomPacketPayload.Type<PacketUpdateSortedPositions> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "update_sorted_positions"));

    public static final StreamCodec<FriendlyByteBuf, PacketUpdateSortedPositions> CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.targetPos);
                buf.writeInt(pkt.positions.size());
                for (BlockPos pos : pkt.positions) {
                    buf.writeBlockPos(pos);
                }
            },
            (buf) -> {
                BlockPos targetPos = buf.readBlockPos();
                int size = buf.readInt();
                List<BlockPos> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(buf.readBlockPos());
                }
                return new PacketUpdateSortedPositions(targetPos, list);
            }
        );

    public PacketUpdateSortedPositions(BlockPos targetPos, List<BlockPos> positions) {
        this.targetPos = targetPos;
        this.positions = positions;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public List<BlockPos> getPositions() {
        return positions;
    }

    @Override
    public Type<PacketUpdateSortedPositions> type() {
        return TYPE;
    }

    public static void handle(PacketUpdateSortedPositions packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(packet.getTargetPos());
            if (be instanceof BlockEntityViaductLinker linker) {
                linker.setSortedTargetPositions(packet.getPositions());
                linker.setChanged();
                player.level().sendBlockUpdated(linker.getBlockPos(), linker.getBlockState(), linker.getBlockState(), 3);
                System.out.println("[Packet] Sortierung empfangen und gespeichert: " + packet.getPositions());
            } else {
                System.out.println("[Packet] Kein ViaductLinker gefunden an: " + packet.getTargetPos());
            }
        });
    }
}