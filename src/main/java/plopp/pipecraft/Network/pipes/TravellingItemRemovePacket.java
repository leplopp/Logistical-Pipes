package plopp.pipecraft.Network.pipes;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plopp.pipecraft.logic.pipe.PipeTravel;

public class TravellingItemRemovePacket implements CustomPacketPayload {
    public final UUID itemId;

    public static final CustomPacketPayload.Type<TravellingItemRemovePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pipecraft", "travelling_item_remove"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, TravellingItemRemovePacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, TravellingItemRemovePacket pkt) {
            buf.writeUUID(pkt.itemId);
        }

        @Override
        public TravellingItemRemovePacket decode(RegistryFriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            return new TravellingItemRemovePacket(id);
        }
    };
    
    
    public TravellingItemRemovePacket(UUID id) {
        this.itemId = id;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TravellingItemRemovePacket pkt, IPayloadContext context) {
        Minecraft.getInstance().execute(() -> {
            try {
                PipeTravel.activeItems.removeIf(item -> item.id.equals(pkt.itemId));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERROR during TravellingItemRemovePacket handle for " + pkt.itemId);
            }
        });
    }
}
