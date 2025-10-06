package plopp.pipecraft.events;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductSpeed;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.speeder.SpeedChangePacket;
import plopp.pipecraft.logic.ViaductTravel;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientRuntimeEvents {
	
	private static boolean wasRightClickPressedLastTick = false;
	private static int lostSightTicks = 0;
	private static int toggleCooldownTicks = 5;
	
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
	public static void onClientTick(ClientTickEvent.Post event) {
	     Minecraft mc = Minecraft.getInstance();
	   
	     if (mc.player == null) return;

	     Player player = mc.player;
	     boolean rightClickPressed = mc.options.keyUse.isDown();

	     if (mc.screen == null && !rightClickPressed && wasRightClickPressedLastTick && toggleCooldownTicks == 0) {
	         HitResult hit = mc.hitResult;
	         if (hit instanceof BlockHitResult bhr) {
	             BlockPos pos = bhr.getBlockPos();
	             BlockState state = mc.level.getBlockState(pos);

	             if (state.getBlock() instanceof BlockViaductSpeed && state.hasProperty(BlockViaductSpeed.SPEED)) {
	                 if (BlockViaductSpeed.editingActive && pos.equals(BlockViaductSpeed.editingPos)) {
	                     BlockViaductSpeed.editingActive = false;
	                     BlockViaductSpeed.editingPos = null;
	                     lostSightTicks = 0;
	                     player.displayClientMessage(Component.translatable("viaduct.speed.change.end"), true);
	                 } else {
	                     BlockViaductSpeed.editingActive = true;
	                     BlockViaductSpeed.editingPos = pos;
	                     lostSightTicks = 0;
	                     player.displayClientMessage(Component.translatable("viaduct.speed.change.start"), true);
	                 }
	                 toggleCooldownTicks = 5; 
	             }
	         }
	     }

	     wasRightClickPressedLastTick = rightClickPressed;

	     if (BlockViaductSpeed.editingActive && BlockViaductSpeed.editingPos != null) {
	         BlockPos pos = BlockViaductSpeed.editingPos;
	         double maxDistance = 6.0;

	         if (player.distanceToSqr(Vec3.atCenterOf(pos)) > maxDistance * maxDistance) {
	             BlockViaductSpeed.editingActive = false;
	             BlockViaductSpeed.editingPos = null;
	             lostSightTicks = 0;
	             player.displayClientMessage(Component.translatable("viaduct.speed.changed.toofar"), true);
	         } else {
	             HitResult hit = mc.hitResult;
	             if (!(hit instanceof BlockHitResult bhr) || !bhr.getBlockPos().equals(pos)) {
	                 lostSightTicks++;
	                 if (lostSightTicks >= 5) {
	                     BlockViaductSpeed.editingActive = false;
	                     BlockViaductSpeed.editingPos = null;
	                     lostSightTicks = 0;
	                     player.displayClientMessage(Component.translatable("viaduct.speed.changed.lookaway"), true);
	                 }
	             } else {
	                 lostSightTicks = 0;
	             }
	         }
	     }
	 

	     
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
	                 float targetPitch = -ViaductTravel.getTravelPitch(player.getUUID(), player.level()); 

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
	     if (scrollStep == 0) return;

	     if (player.isShiftKeyDown()) {
	         scrollStep *= 10;
	     }
	     int currentSpeed = state.getValue(BlockViaductSpeed.SPEED).getValue();
	     int visualSpeed = 129 - currentSpeed; 
	     visualSpeed += scrollStep;            
	     visualSpeed = Mth.clamp(visualSpeed, 1, 128);
	     int newSpeed = 129 - visualSpeed;     

	     SpeedChangePacket pkt = new SpeedChangePacket(BlockViaductSpeed.editingPos, newSpeed - currentSpeed);
	     NetworkHandler.sendToServer(pkt);

	     event.setCanceled(true);
	 
	 }
	 
	 private static final RenderType GLOW_CUBE = RenderType.create(
			    "glow_cube",
			    DefaultVertexFormat.POSITION_COLOR,
			    VertexFormat.Mode.QUADS,
			    256,
			    false, true,
			    RenderType.CompositeState.builder()
			        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
			        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY) 
			        .setLightmapState(RenderStateShard.NO_LIGHTMAP)             
			        .setWriteMaskState(RenderStateShard.COLOR_WRITE)           
			        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)      
			        .createCompositeState(false)
			);
	 
	   @SuppressWarnings("incomplete-switch")
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
	       BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

	       Direction[] allDirections = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	       
	       for (BlockPos pos : BlockPos.betweenClosed(
	    	        mc.player.blockPosition().offset(-16, -16, -16),
	    	        mc.player.blockPosition().offset(16, 16, 16))) {

	    	    BlockState state = level.getBlockState(pos);
	    	    if (!(state.getBlock() instanceof BlockViaductDetector)) continue;
	    	    if (!state.hasProperty(BlockStateProperties.POWERED) || !state.getValue(BlockStateProperties.POWERED)) {
	    	        continue; 
	    	    }
	    	    Direction blockFacing = state.getValue(BlockViaductDetector.FACING);

	    	    List<Direction> directionsToRender;
	    	    switch (blockFacing) {
	    	        case WEST, EAST -> directionsToRender = List.of(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN);
	    	        case NORTH, SOUTH -> directionsToRender = List.of(Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);
	    	        default -> directionsToRender = List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
	    	    }

	    	    for (Direction facing : directionsToRender) {
	    	        double x = pos.getX() + 0.5 - camPos.x;
	    	        double y = pos.getY() + 0.5 - camPos.y;
	    	        double z = pos.getZ() + 0.5 - camPos.z;

	    	        switch (facing) {
	    	            case NORTH -> z -= 0.47;
	    	            case SOUTH -> z += 0.47;
	    	            case WEST  -> x -= 0.47;
	    	            case EAST  -> x += 0.47;
	    	            case UP    -> y += 0.47;
	    	            case DOWN  -> y -= 0.47;
	    	        }

	    	        poseStack.pushPose();
	    	        poseStack.translate(x, y, z);

	    	        float size = -0.2f; 
	    	        poseStack.scale(size, size, size);

	    	        float half = 0.36f;
	    	        float r = 1f, g = 0f, b = 0f, a = 0.1f;
	    	        VertexConsumer buffer = bufferSource.getBuffer(GLOW_CUBE);
	    	        Matrix4f mat = poseStack.last().pose();

	    	        float[][] faces = new float[][] {
	    	            {-half, -half, -half,  half, -half, -half,  half, half, -half, -half, half, -half}, // -Z
	    	            { half, -half,  half, -half, -half,  half, -half, half,  half,  half, half,  half}, // +Z
	    	            {-half, -half,  half, -half, -half, -half, -half, half, -half, -half, half,  half}, // -X
	    	            { half, -half, -half,  half, -half,  half,  half, half,  half,  half, half, -half}, // +X
	    	            {-half, half, -half,  half, half, -half,  half, half,  half, -half, half,  half},   // +Y
	    	            {-half, -half, -half, -half, -half,  half,  half, -half,  half,  half, -half, -half} // -Y
	    	        };

	    	        for (float[] f : faces) {
	    	            buffer.addVertex(mat, f[0], f[1], f[2]).setColor(r,g,b,a);
	    	            buffer.addVertex(mat, f[3], f[4], f[5]).setColor(r,g,b,a);
	    	            buffer.addVertex(mat, f[6], f[7], f[8]).setColor(r,g,b,a);
	    	            buffer.addVertex(mat, f[9], f[10], f[11]).setColor(r,g,b,a);
	    	        }


	    	        poseStack.popPose();
	    	    }
	    	}
	
	       for (BlockPos pos : BlockPos.betweenClosed(
	               mc.player.blockPosition().offset(-16, -16, -16),
	               mc.player.blockPosition().offset(16, 16, 16))) {

	           BlockState state = level.getBlockState(pos);
	           if (!(state.getBlock() instanceof BlockViaductSpeed)) continue;

	           int speed = state.getValue(BlockViaductSpeed.SPEED).getValue();
	           int visualSpeed = 129 - speed; // 1 -> 128, 128 -> 1
	           String text = String.valueOf(visualSpeed);
	           Direction blockFacing = state.getValue(BlockViaductSpeed.FACING);

	           BlockEntity be = level.getBlockEntity(pos);
	           ItemStack idStack = ItemStack.EMPTY;

	  
	           if (be instanceof BlockEntityViaductSpeed speedBE) {
	               idStack = speedBE.getIdStack();
	           }


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

	        	    double xOffset = 0, yOffset = 0, zOffset = 0;

	        	 switch (facing) {
	        	     case NORTH -> z -= 0.49;
	        	     case SOUTH -> z += 0.49;
	        	     case WEST -> x -= 0.49;
	        	     case EAST -> x += 0.49;
	        	     case UP -> y += 0.43;
	        	     case DOWN -> y -= 0.56;
	        	 }

	        	   boolean isHorizontal = blockFacing.getAxis().isHorizontal();

	               if (isHorizontal) {
	                   switch (facing) {
	                       case WEST -> { zOffset += -0.07; yOffset += -0.07; }
	                       case EAST -> { zOffset += -0.07; yOffset += -0.07; }
	                       case NORTH -> { xOffset += -0.07; yOffset += -0.07; }
	                       case SOUTH -> { xOffset += -0.07; yOffset += -0.07; }
	                       case UP -> { if (blockFacing.getAxis() == Direction.Axis.Z) zOffset += -0.06; else xOffset += -0.06; }
	                       case DOWN -> { if (blockFacing.getAxis() == Direction.Axis.Z) zOffset += -0.06; else xOffset += -0.06; }
	                   }
	               }

	               x += xOffset;
	               y += yOffset;
	               z += zOffset;

	               poseStack.pushPose();
	               poseStack.translate(x, y, z);

	               switch (facing) {
	               case UP -> {
	                   poseStack.mulPose(Axis.XP.rotationDegrees(90));
	                   if (blockFacing == Direction.WEST || blockFacing == Direction.EAST) poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
	               }
	               case DOWN -> {
	                   poseStack.mulPose(Axis.XP.rotationDegrees(-90));
	                   if (blockFacing == Direction.NORTH || blockFacing == Direction.SOUTH) poseStack.mulPose(Axis.ZP.rotationDegrees(180));
	                   if (blockFacing == Direction.WEST || blockFacing == Direction.EAST)  poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
	               }
	               default -> {
	                   float rotation = switch (facing) {
	                       case NORTH -> 180f;
	                       case SOUTH -> 0f;
	                       case WEST  -> -90f;
	                       case EAST  -> 90f;
	                       default   -> 0f;
	                   };
	                   poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
	                   poseStack.mulPose(Axis.XP.rotationDegrees(180));

	                   if (isHorizontal) {
	                	    switch (facing) {
	                	        case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));  
	                	        case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90)); 
	                	        case NORTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));  
	                	        case SOUTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
	                	    }
	                	}
	               }
	           }

	               poseStack.pushPose();
	               poseStack.scale(0.016f,0.016f,0.016f);
	               font.drawInBatch(text, -font.width(text)/2f, 0, 0xFFFFFFFF, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
	               poseStack.popPose();

	               if (!idStack.isEmpty()) {
	                   poseStack.pushPose();
	                   poseStack.translate(-0.07, -0.05, 0.03);
	                   poseStack.mulPose(Axis.XP.rotationDegrees(180));

	                   if (idStack.getItem() instanceof BlockItem blockItem) {
	                       poseStack.scale(0.12f, 0.12f, 0.045f);
	                       Block blockToRender = blockItem.getBlock();
	                       dispatcher.renderSingleBlock(blockToRender.defaultBlockState(),poseStack,bufferSource,15728880,OverlayTexture.NO_OVERLAY,ModelData.EMPTY,null);
	                   } else {
	                       poseStack.scale(0.35f, 0.35f, 0.35f);
	                       poseStack.translate(0.16, 0.05, 0.09);
	                       mc.getItemRenderer().renderStatic(idStack, ItemDisplayContext.GROUND,15728880, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, mc.level, 0);
	                   }

	                   poseStack.popPose();
	               }
	               
	               poseStack.popPose();
	           }
	       }
	       ((BufferSource) bufferSource).endBatch();
	   }  
}