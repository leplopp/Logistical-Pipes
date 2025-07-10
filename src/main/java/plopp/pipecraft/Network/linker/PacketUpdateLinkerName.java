package plopp.pipecraft.Network.linker;

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
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;

public record PacketUpdateLinkerName(BlockPos pos, String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PacketUpdateLinkerName> TYPE =
		    new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "linker_name"));

	public static final StreamCodec<FriendlyByteBuf, PacketUpdateLinkerName> CODEC =
		    StreamCodec.<FriendlyByteBuf, PacketUpdateLinkerName>of(
		        (FriendlyByteBuf buf, PacketUpdateLinkerName pkt) -> {
		            buf.writeBlockPos(pkt.pos());
		            buf.writeUtf(pkt.name());
		        },
		        (FriendlyByteBuf buf) -> new PacketUpdateLinkerName(buf.readBlockPos(), buf.readUtf(32767))
		    );

        @Override
        public Type<PacketUpdateLinkerName> type() {
            return TYPE;
        }

        public static void handle(PacketUpdateLinkerName packet, IPayloadContext context) {
            handleInternal(packet, context.connection(), context.player());
        }

        @SuppressWarnings("deprecation")
		private static void handleInternal(PacketUpdateLinkerName packet, Connection connection, Player player) {
            if (player == null) return;
            Level level = player.level();
            if (level.hasChunkAt(packet.pos())) {
                BlockEntity be = level.getBlockEntity(packet.pos());
                if (be instanceof BlockEntityViaductLinker linker) {
                    linker.setCustomName(packet.name());
                    linker.setChanged();
                    level.sendBlockUpdated(packet.pos(), linker.getBlockState(), linker.getBlockState(), 3);
                }
            }
        }
}