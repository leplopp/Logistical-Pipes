package plopp.pipecraft;

import java.util.UUID;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
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

	             if (mc.options.getCameraType().isFirstPerson()) {
	                 float currentYaw = player.getYRot();
	                 float targetYaw = ViaductTravel.getTravelYaw(player.getUUID());
	                 float currentPitch = player.getXRot();
	                 float targetPitch = -ViaductTravel.getTravelPitch(player.getUUID()); // Negiert

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
	    public static void onRenderPlayer(RenderLivingEvent.Pre<?, ?> event) {
	        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
	            return;
	        }

	        if (!ViaductTravel.isTravelActive(player)) {
	            return;
	        }

	        float yaw = ViaductTravel.getTravelYaw(player.getUUID());

	        player.yBodyRot = yaw;
	        player.yBodyRotO = yaw;


	        player.setYHeadRot(yaw);
	        player.yHeadRotO = yaw;
	    }
	    
	    @SubscribeEvent
	    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
	        if (!(event.getEntity() instanceof Player player)) return;
	        Level level = event.getLevel();
	        BlockPos pos = event.getPos();
	        InteractionHand hand = event.getHand();
	        ItemStack stack = player.getItemInHand(hand);
	        BlockState state = level.getBlockState(pos);

	        if (level.isClientSide) return;
	        if (!(state.getBlock() instanceof BlockViaduct)) return;

	        if (stack.getItem() == Items.GLOWSTONE_DUST) {
	            int currentLevel = state.getValue(BlockViaduct.LIGHT_LEVEL);
	            if (player.isShiftKeyDown()) {
	                if (currentLevel < 15) {
	                    int toConsume = 1;
	                    BlockState newState = state.setValue(BlockViaduct.LIGHT_LEVEL, currentLevel + toConsume);
	                    level.setBlock(pos, newState, 3);
	                    if (!player.isCreative()) stack.shrink(toConsume);
	                    player.displayClientMessage(Component.literal("Lichtlevel erhöht auf " + (currentLevel + toConsume)), true);
	                    event.setCancellationResult(InteractionResult.SUCCESS);
	                    event.setCanceled(true);
	                    return;
	                } else {
	                    player.displayClientMessage(Component.literal("Lichtlevel ist bereits auf Maximum (15)."), true);
	                    event.setCancellationResult(InteractionResult.FAIL);
	                    event.setCanceled(true);
	                    return;
	                }
	            } else {
	                if (currentLevel < 15) {
	                    int maxIncrease = 15 - currentLevel;
	                    int toConsume = Math.min(stack.getCount(), maxIncrease);
	                    int newLevel = currentLevel + toConsume;
	                    BlockState newState = state.setValue(BlockViaduct.LIGHT_LEVEL, newLevel);
	                    level.setBlock(pos, newState, 3);
	                    if (!player.isCreative()) stack.shrink(toConsume);
	                    player.displayClientMessage(Component.literal("Lichtlevel erhöht auf " + newLevel), true);
	                    event.setCancellationResult(InteractionResult.SUCCESS);
	                    event.setCanceled(true);
	                    return;
	                } else {
	                    player.displayClientMessage(Component.literal("Lichtlevel ist bereits auf Maximum (15)."), true);
	                    event.setCancellationResult(InteractionResult.FAIL);
	                    event.setCanceled(true);
	                    return;
	                }
	            }
	        }

	        if (stack.getItem() == Items.BRUSH) {
	            int currentLevel = state.getValue(BlockViaduct.LIGHT_LEVEL);
	            if (currentLevel > 0) {
	                BlockState newState = state.setValue(BlockViaduct.LIGHT_LEVEL, 0);
	                level.setBlock(pos, newState, 3);
	                ItemStack glowstoneReturn = new ItemStack(Items.GLOWSTONE_DUST, currentLevel);
	                boolean added = player.getInventory().add(glowstoneReturn);
	                if (!added) {
	                    player.drop(glowstoneReturn, false);
	                }
	                player.displayClientMessage(Component.literal("Lichtlevel auf 0 zurückgesetzt. Glowstone zurückgegeben: " + currentLevel), true);
	                event.setCancellationResult(InteractionResult.SUCCESS);
	                event.setCanceled(true);
	                return;
	            } else {
	                player.displayClientMessage(Component.literal("Lichtlevel ist bereits 0."), true);
	                event.setCancellationResult(InteractionResult.FAIL);
	                event.setCanceled(true);
	                return;
	            }
	        }
	    }
}