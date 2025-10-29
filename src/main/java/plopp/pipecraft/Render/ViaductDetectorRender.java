package plopp.pipecraft.Render;

import java.util.List;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;

@EventBusSubscriber(value = Dist.CLIENT)
public class ViaductDetectorRender {

	private static final RenderType GLOW_CUBE = RenderType.create("glow_cube", DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS, 256, false, true,
			RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
					.setLightmapState(RenderStateShard.NO_LIGHTMAP).setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).createCompositeState(false));

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
			return;

		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null || mc.player == null)
			return;

		PoseStack poseStack = event.getPoseStack();
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();
		MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();

		for (BlockPos pos : BlockPos.betweenClosed(mc.player.blockPosition().offset(-16, -16, -16),
				mc.player.blockPosition().offset(16, 16, 16))) {

			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof BlockViaductDetector))
				continue;
			if (!state.hasProperty(BlockStateProperties.POWERED) || !state.getValue(BlockStateProperties.POWERED)) {
				continue;
			}
			Direction blockFacing = state.getValue(BlockViaductDetector.FACING);

			List<Direction> directionsToRender;
			switch (blockFacing) {
			case WEST, EAST ->
				directionsToRender = List.of(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN);
			case NORTH, SOUTH ->
				directionsToRender = List.of(Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);
			default -> directionsToRender = List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
			}

			for (Direction facing : directionsToRender) {
				double x = pos.getX() + 0.5 - camPos.x;
				double y = pos.getY() + 0.5 - camPos.y;
				double z = pos.getZ() + 0.5 - camPos.z;

				switch (facing) {
				case NORTH -> z -= 0.47;
				case SOUTH -> z += 0.47;
				case WEST -> x -= 0.47;
				case EAST -> x += 0.47;
				case UP -> y += 0.47;
				case DOWN -> y -= 0.47;
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
						{ -half, -half, -half, half, -half, -half, half, half, -half, -half, half, -half }, // -Z
						{ half, -half, half, -half, -half, half, -half, half, half, half, half, half }, // +Z
						{ -half, -half, half, -half, -half, -half, -half, half, -half, -half, half, half }, // -X
						{ half, -half, -half, half, -half, half, half, half, half, half, half, -half }, // +X
						{ -half, half, -half, half, half, -half, half, half, half, -half, half, half }, // +Y
						{ -half, -half, -half, -half, -half, half, half, -half, half, half, -half, -half } // -Y
				};

				for (float[] f : faces) {
					buffer.addVertex(mat, f[0], f[1], f[2]).setColor(r, g, b, a);
					buffer.addVertex(mat, f[3], f[4], f[5]).setColor(r, g, b, a);
					buffer.addVertex(mat, f[6], f[7], f[8]).setColor(r, g, b, a);
					buffer.addVertex(mat, f[9], f[10], f[11]).setColor(r, g, b, a);
				}

				poseStack.popPose();
			}
		}
	}
}