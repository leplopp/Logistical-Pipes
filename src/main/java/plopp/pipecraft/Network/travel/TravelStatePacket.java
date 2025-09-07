package plopp.pipecraft.Network.travel;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.logic.ViaductTravel.VerticalDirection;

public class TravelStatePacket implements CustomPacketPayload {
	
    private final UUID playerUUID;
    private final boolean active;
    private final float travelYaw;
    private final float travelPitch;
    private final VerticalDirection verticalDirection;
    private final boolean resetModel;

    public static final CustomPacketPayload.Type<TravelStatePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "travel_state"));

    public static final StreamCodec<FriendlyByteBuf, TravelStatePacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUUID(pkt.playerUUID);
                buf.writeBoolean(pkt.active);
                buf.writeFloat(pkt.travelYaw);
                buf.writeFloat(pkt.travelPitch);
                buf.writeEnum(pkt.verticalDirection);
                buf.writeBoolean(pkt.resetModel);
            },
            (buf) -> new TravelStatePacket(
                buf.readUUID(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readEnum(VerticalDirection.class),
                buf.readBoolean()
            )
        );

    public TravelStatePacket(UUID playerUUID, boolean active, float travelYaw, float travelPitch, VerticalDirection verticalDirection, boolean resetModel) {
        this.playerUUID = playerUUID;
        this.active = active;
        this.travelYaw = travelYaw;
        this.travelPitch = travelPitch;
        this.verticalDirection = verticalDirection;
        this.resetModel = resetModel;

    }

    public UUID getPlayerUUID() { return playerUUID; }
    public boolean isActive() { return active; }
    public float getTravelYaw() { return travelYaw; }
    public float getTravelPitch() { return travelPitch; }
    public VerticalDirection getVerticalDirection() { return verticalDirection; }
    public boolean shouldResetModel() { return resetModel; }

    @Override
    public CustomPacketPayload.Type<TravelStatePacket> type() {
        return TYPE;
    }

    public static void handle(TravelStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            UUID uuid = packet.getPlayerUUID();

            ClientTravelDataManager.updatePlayerTravelData(packet);

            if (packet.shouldResetModel()) {
                ViaductTravel.markResetModel(uuid);
            }
        });
    }
    
    public static TravelStatePacket empty(UUID playerUUID) {
        return new TravelStatePacket(playerUUID, false, 0f, 0f, VerticalDirection.NONE, true);
    }
}