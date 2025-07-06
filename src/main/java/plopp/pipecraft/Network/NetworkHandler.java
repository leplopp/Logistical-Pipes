package plopp.pipecraft.Network;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Network.travel.PacketTravelStart;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
	
	@SubscribeEvent
	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
		 System.out.println("[NetworkHandler] registerPayloadHandlers wurde aufgerufen");
		 event.registrar(PipeCraftIndex.MODID)
		    .playToServer(PacketUpdateLinkerName.TYPE, PacketUpdateLinkerName.CODEC, PacketUpdateLinkerName::handle)
		    .playToServer(PacketTravelStart.TYPE, PacketTravelStart.CODEC, PacketTravelStart::handle)
		    .playToClient(ViaductLinkerListPacket.TYPE, ViaductLinkerListPacket.CODEC, ViaductLinkerListPacket::handle)
		    
		    // ✅ HINZUGEFÜGT:
		    .playToServer(PacketUpdateSortedPositions.TYPE, PacketUpdateSortedPositions.CODEC, PacketUpdateSortedPositions::handle);
		}
	
	    public static void sendNameToServer(BlockPos pos, String name) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(new PacketUpdateLinkerName(pos, name));
	        }
	    }
	    
	    public static void sendTravelStartPacket(BlockPos start, BlockPos target) {
	        var connection = Minecraft.getInstance().getConnection();
	        System.out.println("[Client] Sende TravelStart von " + start + " zu " + target + ", connection: " + connection);
	        if (connection != null) {
	            connection.send(new PacketTravelStart(start, target));
	        } else {
	            System.out.println("[Client] Verbindung null, Packet wird nicht gesendet!");
	        }
	    }
	    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
	        player.connection.send(packet);
	    }
	    
	    public static void sendSortedPositionsToServer(BlockPos pos, List<BlockPos> sortedPositions) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(new PacketUpdateSortedPositions(pos, sortedPositions));
	            System.out.println("[Client] Sende sortierte Positionen an Server: " + sortedPositions);
	        } else {
	            System.out.println("[Client] Verbindung null – sortierte Positionen wurden nicht gesendet.");
	        }
	    }
	    public static void sendToServer(CustomPacketPayload packet) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(packet);
	        } else {
	            System.out.println("[Client] Verbindung null – Packet nicht gesendet: " + packet);
	        }
	    }
	}