package com.plopp.pipecraft.Network;

import com.plopp.pipecraft.PipeCraftIndex;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
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

public record PacketUpdateLinkerName(BlockPos pos, String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PacketUpdateLinkerName> TYPE =
		    new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "linker_name"));

	public static final StreamCodec<FriendlyByteBuf, PacketUpdateLinkerName> CODEC =
		    StreamCodec.<FriendlyByteBuf, PacketUpdateLinkerName>of(
		        // Encoder: schreibt in Buffer
		        (FriendlyByteBuf buf, PacketUpdateLinkerName pkt) -> {
		            buf.writeBlockPos(pkt.pos());
		            buf.writeUtf(pkt.name());
		        },
		        // Decoder: liest aus Buffer und erstellt neues Packet
		        (FriendlyByteBuf buf) -> new PacketUpdateLinkerName(buf.readBlockPos(), buf.readUtf(32767))
		    );

        @Override
        public Type<PacketUpdateLinkerName> type() {
            return TYPE;
        }

        public static void handle(PacketUpdateLinkerName packet, IPayloadContext context) {
            handleInternal(packet, context.connection(), context.player());
        }

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
