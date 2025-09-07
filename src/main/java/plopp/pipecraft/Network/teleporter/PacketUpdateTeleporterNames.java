package plopp.pipecraft.Network.teleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductTeleporter;

public record PacketUpdateTeleporterNames(BlockPos pos, String startName, String targetName, String targetId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketUpdateTeleporterNames> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "teleporter_names"));

    public static final StreamCodec<FriendlyByteBuf, PacketUpdateTeleporterNames> CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos());
                buf.writeUtf(pkt.startName());
                buf.writeUtf(pkt.targetName());
                buf.writeUtf(pkt.targetId());
            },
            buf -> new PacketUpdateTeleporterNames(buf.readBlockPos(), buf.readUtf(32767), buf.readUtf(32767), buf.readUtf(32767))
        );

    @Override
    public Type<PacketUpdateTeleporterNames> type() {
        return TYPE;
    }

    public static void handle(PacketUpdateTeleporterNames packet, IPayloadContext context) {
        handleInternal(packet, context.connection(), context.player());
    }

    private static void handleInternal(PacketUpdateTeleporterNames packet, Connection connection, Player player) {
        if (player == null) return;
        Level level = player.level();
        if (level.hasChunkAt(packet.pos())) {
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof BlockEntityViaductTeleporter teleporter) {
                teleporter.setStartName(packet.startName());
                teleporter.setTargetName(packet.targetName());
                teleporter.setTargetId(packet.targetId());
                teleporter.setChanged();
                level.sendBlockUpdated(packet.pos(), teleporter.getBlockState(), teleporter.getBlockState(), 3);
            }
        }
    }
}