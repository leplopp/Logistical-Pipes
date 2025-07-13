package plopp.pipecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.logic.ViaductTravel;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientRuntimeEvents {
	private static final Map<UUID, Integer> lastShownProgress = new HashMap<>();

	@SubscribeEvent
	public static void onClientPlayerTick(PlayerTickEvent.Post event) {
	    Player player = event.getEntity();
	    if (!(player instanceof LocalPlayer localPlayer)) return;

	    UUID id = localPlayer.getUUID();

	    if (ViaductTravel.shouldTriggerJump(id)) {
	        localPlayer.jumpFromGround();
	    }

	    if (!ViaductTravel.isTravelActive(localPlayer)) {
	        lastShownProgress.remove(id);
	        return;
	    }

	    int progress = ViaductTravel.getChargeProgress(localPlayer);
	    Integer lastProgress = lastShownProgress.getOrDefault(id, -1);

	    if (progress < 100) {
	        if (progress != lastProgress) {
	            localPlayer.displayClientMessage(Component.literal("Charging: " + progress + "%"), true);
	            lastShownProgress.put(id, progress);
	        }
	    } else if (lastProgress != 100) {
	        localPlayer.displayClientMessage(Component.literal("Charging: 100%"), true);
	        lastShownProgress.put(id, 100);
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

	             if (mc.options.getCameraType().isFirstPerson()) {
	                 float currentYaw = player.getYRot();
	                 float targetYaw = ViaductTravel.getTravelYaw(player.getUUID(),  player.level());
	                 float currentPitch = player.getXRot();
	                 float targetPitch = -ViaductTravel.getTravelPitch(player.getUUID(), player.level()); // Negiert

	                 float smoothFactor = 0.5f;

	                 float newYaw = lerpAngle(currentYaw, targetYaw, smoothFactor);
	                 float newPitch = lerpAngle(currentPitch, targetPitch, smoothFactor);

	                 player.setYRot(newYaw);
	                 player.setYHeadRot(newYaw);
	                 player.setXRot(newPitch);
	                 player.yBodyRot = newYaw;
	             }
	         }
	     }
	 }
	 
	 private static float lerpAngle(float from, float to, float alpha) {
		    float difference = ((to - from + 540) % 360) - 180;
		    return (from + alpha * difference) % 360;
	}
	 
	 @SubscribeEvent
	 public static void onRenderPlayerPre(RenderLivingEvent.Pre<?, ?> event) {
	     if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
	     if (!(event.getRenderer() instanceof PlayerRenderer renderer)) return;
	     if (!ViaductTravel.isTravelActive(player)) return;

	     float yaw = ViaductTravel.getTravelYaw(player.getUUID(), player.level());
	     player.yBodyRot = yaw;
	     player.yBodyRotO = yaw;
	     player.setYHeadRot(yaw);
	     player.yHeadRotO = yaw;
	 }
}