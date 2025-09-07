package plopp.pipecraft.Network.travel;

import java.util.UUID;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;

public record PacketTravelStop(UUID playerUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTravelStop> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "travel_stop"));

    public static final StreamCodec<FriendlyByteBuf, PacketTravelStop> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeUUID(pkt.playerUUID()),
                    buf -> new PacketTravelStop(buf.readUUID())
            );

    @Override
    public Type<PacketTravelStop> type() {
        return TYPE;
    }

    public static void handle(PacketTravelStop packet, IPayloadContext context) {
        // Nur Client
        if (context.player() instanceof LocalPlayer player) {
            // Dimensions zurücksetzen
            player.refreshDimensions();
            EntityDimensions dim = player.getDimensions(Pose.STANDING);

            double w = dim.width();
            double h = dim.height();

            player.setBoundingBox(new AABB(
                player.getX() - w / 2, player.getY(), player.getZ() - w / 2,
                player.getX() + w / 2, player.getY() + h, player.getZ() + w / 2
            ));
        }
    }
}