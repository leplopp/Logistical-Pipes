package com.plopp.pipecraft;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
	   @SubscribeEvent
	    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
	        event.registrar(PipeCraftIndex.MODID)
	            .playToServer(PacketUpdateLinkerName.TYPE, PacketUpdateLinkerName.CODEC, PacketUpdateLinkerName::handle);
	    }

	    public static void sendNameToServer(BlockPos pos, String name) {
	        var connection = Minecraft.getInstance().getConnection();
	        if (connection != null) {
	            connection.send(new PacketUpdateLinkerName(pos, name));
	        }
	    }
	}