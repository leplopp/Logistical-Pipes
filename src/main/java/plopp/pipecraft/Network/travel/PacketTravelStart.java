package plopp.pipecraft.Network.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.logic.ViaductTravel;

public record PacketTravelStart(BlockPos startPos, BlockPos targetPos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PacketTravelStart> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "travel_start"));

	public static final StreamCodec<FriendlyByteBuf, PacketTravelStart> CODEC = StreamCodec.of((buf, pkt) -> {
		buf.writeBlockPos(pkt.startPos());
		buf.writeBlockPos(pkt.targetPos());
	}, (buf) -> new PacketTravelStart(buf.readBlockPos(), buf.readBlockPos()));

	@Override
	public Type<PacketTravelStart> type() {
		return TYPE;
	}

	public static void handle(PacketTravelStart packet, IPayloadContext context) {

		Player player = context.player();
		if (!(player instanceof ServerPlayer serverPlayer))
			return;

		serverPlayer.level().getServer().execute(() -> {
			ViaductTravel.start(serverPlayer, packet.startPos(), packet.targetPos(), 32);
			NetworkHandler.sendTravelStateToAll(serverPlayer, false);
			serverPlayer.displayClientMessage(Component.translatable("viaduct.travel.start"), true);
		});
	}
}