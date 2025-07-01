package com.plopp.pipecraft.Network;

import com.plopp.pipecraft.PipeCraftIndex;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
	
	@SubscribeEvent
	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
		 System.out.println("[NetworkHandler] registerPayloadHandlers wurde aufgerufen");
		 event.registrar(PipeCraftIndex.MODID)
		    .playToServer(PacketUpdateLinkerName.TYPE, PacketUpdateLinkerName.CODEC, PacketUpdateLinkerName::handle)
		    .playToServer(PacketTravelStart.TYPE, PacketTravelStart.CODEC, (packet, context) -> {
		        PacketTravelStart.handle(packet, context);
		    })
		    .playToClient(ViaductLinkerListPacket.TYPE, ViaductLinkerListPacket.CODEC, ViaductLinkerListPacket::handle);
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
	}