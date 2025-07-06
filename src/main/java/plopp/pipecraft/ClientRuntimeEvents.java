package plopp.pipecraft;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import plopp.pipecraft.logic.ViaductTravel;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientRuntimeEvents {
	
	 @SubscribeEvent
		public static void onClientPlayerTick(PlayerTickEvent.Post event) {
		    Player player = event.getEntity();
		    if (!(player instanceof LocalPlayer localPlayer)) return;

		    UUID id = localPlayer.getUUID();
		    if (ViaductTravel.shouldTriggerJump(id)) {
		        localPlayer.jumpFromGround();
		    }
		}
	    @SubscribeEvent
	    public static void onClientTick(ClientTickEvent.Pre event) {
	        Minecraft mc = Minecraft.getInstance();
	        if (mc.player == null) return;

	        Player player = mc.player;

	        if (ViaductTravel.isTravelActive(player)) {
	            player.setDeltaMovement(Vec3.ZERO);

	            if (player instanceof LocalPlayer localPlayer) {
	                localPlayer.input.forwardImpulse = 0;
	                localPlayer.input.leftImpulse = 0;
	                localPlayer.input.jumping = false;
	                localPlayer.input.shiftKeyDown = false;
	                player.setOnGround(true);
	                
	            }
	      } 
	}
}
