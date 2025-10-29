package plopp.pipecraft.Network.teleporter;

import net.minecraft.core.BlockPos;
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

public record PacketUpdateTeleporterToggle(BlockPos pos, boolean visible) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PacketUpdateTeleporterToggle> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "teleport_id_visible"));

	public static final StreamCodec<FriendlyByteBuf, PacketUpdateTeleporterToggle> CODEC = StreamCodec
			.of((buf, pkt) -> {
				buf.writeBlockPos(pkt.pos());
				buf.writeBoolean(pkt.visible());
			}, buf -> new PacketUpdateTeleporterToggle(buf.readBlockPos(), buf.readBoolean()));

	@Override
	public Type<PacketUpdateTeleporterToggle> type() {
		return TYPE;
	}

	@SuppressWarnings("deprecation")
	public static void handle(PacketUpdateTeleporterToggle packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			if (player == null)
				return;
			Level level = player.level();
			if (level.hasChunkAt(packet.pos())) {
				BlockEntity be = level.getBlockEntity(packet.pos());
				if (be instanceof BlockEntityViaductTeleporter teleporter) {
					teleporter.setTeleportIdVisible(packet.visible());
					level.sendBlockUpdated(packet.pos(), teleporter.getBlockState(), teleporter.getBlockState(), 3);
				}
			}
		});
	}
}