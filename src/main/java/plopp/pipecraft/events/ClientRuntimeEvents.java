package plopp.pipecraft.events;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.SpeedChangePacket;
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
	 
	 @SuppressWarnings("unused")
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
	 
	   @SubscribeEvent
	    public static void onScroll(InputEvent.MouseScrollingEvent event) {
	        Minecraft mc = Minecraft.getInstance();
	        Level level = mc.level;
	        Player player = mc.player;

	        if (!BlockViaductSpeed.editingActive || BlockViaductSpeed.editingPos == null || level == null || player == null) return;

	        BlockState state = level.getBlockState(BlockViaductSpeed.editingPos);

	        if (!(state.getBlock() instanceof BlockViaductSpeed) || !state.hasProperty(BlockViaductSpeed.SPEED)) return;

	        int scrollStep = (int) Math.signum(event.getScrollDeltaY());
	        if(scrollStep == 0) return;

	        // Sende Packet an Server mit Position und Delta
	        SpeedChangePacket pkt = new SpeedChangePacket(BlockViaductSpeed.editingPos, scrollStep);
	        NetworkHandler.sendToServer(pkt); // Deine Methode zum Senden des Packets

	        event.setCanceled(true);
	    }
	   
	 @SubscribeEvent
	 public static void onRenderLevel(RenderLevelStageEvent event) {
	     if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

	     Minecraft mc = Minecraft.getInstance();
	     Level level = mc.level;
	     if (level == null || mc.player == null) return;

	     PoseStack poseStack = event.getPoseStack();
	     Camera camera = mc.gameRenderer.getMainCamera();
	     Vec3 camPos = camera.getPosition();
	     MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();
	     Font font = mc.font;

	     Direction[] allDirections = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	     for (BlockPos pos : BlockPos.betweenClosed(
	             mc.player.blockPosition().offset(-16, -16, -16),
	             mc.player.blockPosition().offset(16, 16, 16))) {

	         BlockState state = level.getBlockState(pos);
	         if (!(state.getBlock() instanceof BlockViaductSpeed)) continue;

	         int speed = state.getValue(BlockViaductSpeed.SPEED).getValue();
	         String text = String.valueOf(speed);

	         Direction blockFacing = state.getValue(BlockViaductSpeed.FACING);

	         List<Direction> directionsToRender;
	         switch (blockFacing) {
	             case WEST, EAST -> directionsToRender = List.of(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN);
	             case NORTH, SOUTH -> directionsToRender = List.of(Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);
	             default -> directionsToRender = Arrays.asList(allDirections);
	         }

	         for (Direction facing : directionsToRender) {
	        	    double x = pos.getX() + 0.5 - camPos.x;
	        	    double y = pos.getY() + 0.56 - camPos.y;
	        	    double z = pos.getZ() + 0.5 - camPos.z;

	        	    switch (facing) {
	        	        case NORTH -> z -= 0.49;
	        	        case SOUTH -> z += 0.49;
	        	        case WEST  -> x -= 0.49;
	        	        case EAST  -> x += 0.49;
	        	        case UP -> {
	        	            y += 0.43;

	        	            double xShift = -0.06;
	        	            double zShift = 0.0;

	        	            if (blockFacing == Direction.NORTH || blockFacing == Direction.SOUTH) {
	        	                xShift = 0.00;  // Beispielwert für NORD/SÜD
	        	                zShift = -0.06;  // Beispielwert für NORD/SÜD
	        	            }
	        	            x += xShift;
	        	            z += zShift;
	        	        }
	        	        case DOWN -> {
	        	            y -= 0.56;

	        	            double xShift = 0.06;
	        	            double zShift = 0.0;

	        	            if (blockFacing == Direction.NORTH || blockFacing == Direction.SOUTH) {
	        	                xShift = -0.00;  // Beispielwert für NORD/SÜD
	        	                zShift = -0.06;  // Beispielwert für NORD/SÜD
	        	            }
	        	            x += xShift;
	        	            z += zShift;
	        	        }
	        	        default -> {}
	        	    }

	        	    poseStack.pushPose();
	        	    poseStack.translate(x, y, z);

	        	    if (facing == Direction.UP) {

	        	        float radiansY = (float) Math.toRadians(90);
	        	        Quaternionf quatY = new Quaternionf().fromAxisAngleRad(0, 1, 0, radiansY);
	        	        poseStack.mulPose(quatY);

	        	        float radiansX = (float) Math.toRadians(90);
	        	        Quaternionf quatX = new Quaternionf().fromAxisAngleRad(1, 0, 0, radiansX);
	        	        poseStack.mulPose(quatX);

	        	        if (blockFacing == Direction.NORTH || blockFacing == Direction.SOUTH) {

	        	            float extraRotation = (float) Math.toRadians(90);
	        	            Quaternionf extraQuat = new Quaternionf().fromAxisAngleRad(0, 0, 1, extraRotation);
	        	            poseStack.mulPose(extraQuat);
	        	        }
	        	    } else if (facing == Direction.DOWN) {

	        	        float radiansY = (float) Math.toRadians(90);
	        	        Quaternionf quatY = new Quaternionf().fromAxisAngleRad(0, 1, 0, radiansY);
	        	        poseStack.mulPose(quatY);

	        	        float radiansX = (float) Math.toRadians(-90);
	        	        Quaternionf quatX = new Quaternionf().fromAxisAngleRad(1, 0, 0, radiansX);
	        	        poseStack.mulPose(quatX);

	        	        if (blockFacing == Direction.NORTH || blockFacing == Direction.SOUTH) {

	        	            float extraRotation = (float) Math.toRadians(90);
	        	            Quaternionf extraQuat = new Quaternionf().fromAxisAngleRad(0, 0, 1, extraRotation);
	        	            poseStack.mulPose(extraQuat);
	        	        }
	        	    } else {

	        	        float rotationDegrees = switch (facing) {
	        	            case NORTH -> 180f;
	        	            case SOUTH -> 0f;
	        	            case WEST  -> -90f;
	        	            case EAST  -> 90f;
	        	            default -> 0f;
	        	        };
	        	        float radiansY = (float) Math.toRadians(rotationDegrees);
	        	        Quaternionf quatY = new Quaternionf().fromAxisAngleRad(0, 1, 0, radiansY);
	        	        poseStack.mulPose(quatY);

	        	        float radiansX = (float) Math.toRadians(180);
	        	        Quaternionf quatX = new Quaternionf().fromAxisAngleRad(1, 0, 0, radiansX);
	        	        poseStack.mulPose(quatX);
	        	    }

	        	    poseStack.scale(0.02f, 0.02f, 0.02f);

	        	    int color = 0xFFFFFFFF;
	        	    font.drawInBatch(text, -font.width(text) / 2f, 0, color, false,
	        	            poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

	        	    poseStack.popPose();
	        	}
	     }

	     ((BufferSource) bufferSource).endBatch();
	 }
}