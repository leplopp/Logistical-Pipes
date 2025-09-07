package plopp.pipecraft.Network.linker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;

public record PacketCancelScan(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketCancelScan> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "cancel_scan"));

    public static final StreamCodec<FriendlyByteBuf, PacketCancelScan> CODEC =
            StreamCodec.of(
                (buf, packet) -> buf.writeBlockPos(packet.pos()),
                buf -> new PacketCancelScan(buf.readBlockPos())
            );

    @Override
    public Type<PacketCancelScan> type() {
        return TYPE;
    }

    public static void handle(PacketCancelScan packet, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        serverPlayer.getServer().execute(() -> {
            Level level = serverPlayer.level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof BlockEntityViaductLinker linker && linker.asyncScanner != null) {
                linker.asyncScanner = null;
                linker.setAsyncScanInProgress(false);
                System.out.println("[Server] Scan abgebrochen für Linker bei: " + packet.pos());
            }
        });
    }
}