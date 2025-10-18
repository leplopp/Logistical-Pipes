package plopp.pipecraft.Network.linker;

import java.util.UUID;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;

public record PacketTravelRotate(UUID playerUUID, float yaw, float pitch) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTravelRotate> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "travel_rotate"));

    public static final StreamCodec<FriendlyByteBuf, PacketTravelRotate> CODEC =
            StreamCodec.of(
                (buf, pkt) -> {
                    buf.writeUUID(pkt.playerUUID());
                    buf.writeFloat(pkt.yaw());
                    buf.writeFloat(pkt.pitch());
                },
                buf -> new PacketTravelRotate(buf.readUUID(), buf.readFloat(), buf.readFloat())
            );

    @Override
    public Type<PacketTravelRotate> type() {
        return TYPE;
    }

    public static void handle(PacketTravelRotate packet, IPayloadContext context) {
        // Client-seitig ausführen
        if (context.player() instanceof LocalPlayer player) {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                // Nur in First Person anwenden
                player.setYRot(packet.yaw());
                player.setYHeadRot(packet.yaw());
                player.setXRot(packet.pitch());
            }
        }
    }
}
