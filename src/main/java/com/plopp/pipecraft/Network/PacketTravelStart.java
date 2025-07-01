package com.plopp.pipecraft.Network;

import com.plopp.pipecraft.PipeCraftIndex;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import com.plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;
import com.plopp.pipecraft.logic.ViaductTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketTravelStart(BlockPos startPos, BlockPos targetPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTravelStart> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "travel_start"));

        public static final StreamCodec<FriendlyByteBuf, PacketTravelStart> CODEC =
            StreamCodec.of(
            		 (buf, pkt) -> {
            		        buf.writeBlockPos(pkt.startPos());
            		        buf.writeBlockPos(pkt.targetPos());
            		    },
            		    (buf) -> new PacketTravelStart(buf.readBlockPos(), buf.readBlockPos())
            );

        @Override
        public Type<PacketTravelStart> type() {
            return TYPE;
        }

        public static void handle(PacketTravelStart packet, IPayloadContext context) {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                System.out.println("[TravelStart] Spieler ist kein ServerPlayer!");
                return;
            }

            serverPlayer.level().getServer().execute(() -> {
                System.out.println("[TravelStart] Packet erhalten Start: " + packet.startPos() + " Ziel: " + packet.targetPos());

                BlockEntity be = serverPlayer.level().getBlockEntity(packet.startPos());
                if (be instanceof BlockEntityViaductLinker linker) {
                    System.out.println("[TravelStart] Linker found at startPos, linkedTargets: " + linker.getLinkedTargets());
                } else {
                    System.out.println("[TravelStart] Kein Linker an startPos gefunden!");
                }

                ViaductTravel.start(serverPlayer, packet.startPos(), packet.targetPos(), 32);

                serverPlayer.displayClientMessage(Component.literal("Fahrt gestartet."), true);
                serverPlayer.closeContainer();
            });
        }
}