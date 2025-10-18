package plopp.pipecraft.Network.linker;

import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;

public record PacketTravelJump(UUID playerUUID) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketTravelJump> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "travel_jump"));

    public static final StreamCodec<FriendlyByteBuf, PacketTravelJump> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeUUID(pkt.playerUUID()),
                    buf -> new PacketTravelJump(buf.readUUID())
            );

    @Override
    public Type<PacketTravelJump> type() {
        return TYPE;
    }

    public static void handle(PacketTravelJump packet, IPayloadContext context) {

        if (context.player() instanceof LocalPlayer player) {

           player.jumpFromGround();
        }
    }
}
