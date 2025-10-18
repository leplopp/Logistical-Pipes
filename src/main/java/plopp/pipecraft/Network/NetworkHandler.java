package plopp.pipecraft.Network;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Network.linker.PacketCancelScan;
import plopp.pipecraft.Network.linker.PacketTravelJump;
import plopp.pipecraft.Network.linker.PacketTravelRotate;
import plopp.pipecraft.Network.linker.PacketUpdateLinkerName;
import plopp.pipecraft.Network.linker.PacketUpdateSortedPositions;
import plopp.pipecraft.Network.linker.ViaductLinkerListPacket;
import plopp.pipecraft.Network.speeder.SpeedChangePacket;
import plopp.pipecraft.Network.teleporter.PacketUpdateTeleporterNames;
import plopp.pipecraft.Network.teleporter.PacketUpdateTeleporterToggle;
import plopp.pipecraft.Network.travel.PacketTravelStart;
import plopp.pipecraft.Network.travel.PacketTravelStop;
import plopp.pipecraft.Network.travel.TravelStatePacket;
import plopp.pipecraft.logic.ViaductTravel;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT, Dist.DEDICATED_SERVER})
public class NetworkHandler {
	
	@SubscribeEvent
	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
	    
	    event.registrar(PipeCraftIndex.MODID)
	    
	        .playToClient(ViaductLinkerListPacket.TYPE, ViaductLinkerListPacket.CODEC, ViaductLinkerListPacket::handle)
	        .playToClient(TravelStatePacket.TYPE, TravelStatePacket.CODEC, TravelStatePacket::handle)
	        .playToServer(PacketTravelStart.TYPE, PacketTravelStart.CODEC, PacketTravelStart::handle)
	        .playToServer(PacketUpdateSortedPositions.TYPE, PacketUpdateSortedPositions.CODEC, PacketUpdateSortedPositions::handle)
	        .playToServer(PacketUpdateLinkerName.TYPE, PacketUpdateLinkerName.CODEC, PacketUpdateLinkerName::handle)
	        .playToServer(SpeedChangePacket.TYPE,SpeedChangePacket.CODEC,SpeedChangePacket::handle)
	    	.playToServer(PacketUpdateTeleporterNames.TYPE, PacketUpdateTeleporterNames.CODEC, PacketUpdateTeleporterNames::handle)
	    	.playToServer(PacketUpdateTeleporterToggle.TYPE, PacketUpdateTeleporterToggle.CODEC, PacketUpdateTeleporterToggle::handle)
	    	.playToServer(PacketCancelScan.TYPE, PacketCancelScan.CODEC, PacketCancelScan::handle)
	    	.playToClient(PacketTravelStop.TYPE, PacketTravelStop.CODEC, PacketTravelStop::handle)
	    	.playToClient(PacketTravelJump.TYPE, PacketTravelJump.CODEC, PacketTravelJump::handle)
	    	.playToClient(PacketTravelRotate.TYPE, PacketTravelRotate.CODEC, PacketTravelRotate::handle);
	}
	
	    public static void sendNameToServer(BlockPos pos, String name) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(new PacketUpdateLinkerName(pos, name));
	        }
	    }
	    
	    public static void sendTravelStartPacket(BlockPos start, BlockPos target) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(new PacketTravelStart(start, target));
	        } else {
	        }
	    }
	    
	    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
	        player.connection.send(packet);
	    }
	    
	    public static void sendSortedPositionsToServer(BlockPos pos, List<BlockPos> sortedPositions) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(new PacketUpdateSortedPositions(pos, sortedPositions));
	        } 
	    }
	    
	    public static void sendToServer(CustomPacketPayload packet) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(packet);
	        }
	    }
	    
	    public static void sendTravelStateToAll(ServerPlayer sender, boolean resetModel) {
			TravelStatePacket packet = new TravelStatePacket(
	            sender.getUUID(),
	            ViaductTravel.isTravelActive(sender),
	            ViaductTravel.getTravelYaw(sender.getUUID(), sender.level()),
	            ViaductTravel.getTravelPitch(sender.getUUID(), sender.level()),
	            ViaductTravel.getVerticalDirection(sender.getUUID(), sender.level()),
	            resetModel 
	        );

	        MinecraftServer server = sender.getServer();
	        if (server == null) {
	            return;
	        }

	        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
	            player.connection.send(packet);
	        }
	    }
}