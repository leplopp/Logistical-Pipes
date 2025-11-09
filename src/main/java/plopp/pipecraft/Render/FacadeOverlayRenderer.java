package plopp.pipecraft.Render;

import java.util.Collection;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Facade.BlockViaductFacade;
import plopp.pipecraft.logic.Manager.FacadeOverlayManager;
import plopp.pipecraft.model.obj.DynamicColorWrappedModel;

@EventBusSubscriber(value = Dist.CLIENT)
public class FacadeOverlayRenderer {

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null)
			return;

		PoseStack poseStack = event.getPoseStack();
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();

		BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

		Collection<Map.Entry<BlockPos, FacadeOverlayManager.FacadeData>> all = FacadeOverlayManager.getAll();
		if (all.isEmpty())
			return;

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

		for (var entry : all) {
			BlockPos pos = entry.getKey();
			FacadeOverlayManager.FacadeData data = entry.getValue();

			ModelData modelData = ModelData.builder().with(DynamicColorWrappedModel.COLOR_MODEL_DATA_KEY, data.color())
					.build();

			BlockState baseState = BlockRegister.VIADUCTFACADE.get().defaultBlockState()
					.setValue(BlockViaductFacade.CONNECTED_UP, FacadeOverlayManager.hasFacade(pos.above()))
					.setValue(BlockViaductFacade.CONNECTED_DOWN, FacadeOverlayManager.hasFacade(pos.below()))
					.setValue(BlockViaductFacade.CONNECTED_NORTH, FacadeOverlayManager.hasFacade(pos.north()))
					.setValue(BlockViaductFacade.CONNECTED_SOUTH, FacadeOverlayManager.hasFacade(pos.south()))
					.setValue(BlockViaductFacade.CONNECTED_EAST, FacadeOverlayManager.hasFacade(pos.east()))
					.setValue(BlockViaductFacade.CONNECTED_WEST, FacadeOverlayManager.hasFacade(pos.west()));

			int light = LevelRenderer.getLightColor(level, pos);

			poseStack.pushPose();
			poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

			try {

				BakedModel model = dispatcher.getBlockModel(baseState);
				RenderType baseType = RenderType.translucent();
				VertexConsumer consumer = bufferSource.getBuffer(baseType);
				dispatcher.getModelRenderer().renderModel(poseStack.last(), consumer, baseState, model, 1f, 1f, 1f,
						light, OverlayTexture.NO_OVERLAY, modelData, baseType);

				VertexConsumer armConsumer = bufferSource
						.getBuffer(data.transparent() ? RenderType.translucent() : RenderType.cutout());
				renderConnections(level, poseStack, armConsumer, pos, 1f, 1f, 1f, data.transparent(), light);

			} catch (Exception e) {
				System.err.println("[FacadeRender] Fehler beim Rendern: " + e.getMessage());
				e.printStackTrace();
			}

			poseStack.popPose();
		}

		poseStack.popPose();

		try {
			bufferSource.endBatch();
		} catch (Exception e) {
			System.err.println("[FacadeRender] Fehler bei endBatch(): " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void markForReRender(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			BlockState state = mc.level.getBlockState(pos);
			mc.level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	private static void renderConnections(Level level, PoseStack poseStack, VertexConsumer consumer, BlockPos pos,
			float r, float g, float b, boolean transparent, int light) {
		float alpha = transparent ? 0.4f : 1.0f;
		PoseStack.Pose pose = poseStack.last();

		boolean north = FacadeOverlayManager.hasFacade(pos.north());
		boolean south = FacadeOverlayManager.hasFacade(pos.south());
		boolean east = FacadeOverlayManager.hasFacade(pos.east());
		boolean west = FacadeOverlayManager.hasFacade(pos.west());
		boolean up = FacadeOverlayManager.hasFacade(pos.above());
		boolean down = FacadeOverlayManager.hasFacade(pos.below());

		if (north)
			renderArm(pose, consumer, Direction.NORTH, r, g, b, alpha, light);
		if (south)
			renderArm(pose, consumer, Direction.SOUTH, r, g, b, alpha, light);
		if (east)
			renderArm(pose, consumer, Direction.EAST, r, g, b, alpha, light);
		if (west)
			renderArm(pose, consumer, Direction.WEST, r, g, b, alpha, light);
		if (up)
			renderArm(pose, consumer, Direction.UP, r, g, b, alpha, light);
		if (down)
			renderArm(pose, consumer, Direction.DOWN, r, g, b, alpha, light);
	}

	private static void renderArm(PoseStack.Pose pose, VertexConsumer consumer, Direction dir, float r, float g,
			float b, float a, int light) {

		float x1 = 4f, y1 = 4f, z1 = 4f;
		float x2 = 12f, y2 = 12f, z2 = 12f;

		switch (dir) {
		case NORTH -> z1 = 0;
		case SOUTH -> z2 = 16;
		case WEST -> x1 = 0;
		case EAST -> x2 = 16;
		case UP -> y2 = 16;
		case DOWN -> y1 = 0;
		}

		renderCube(pose, consumer, x1, y1, z1, x2, y2, z2, r, g, b, a, light);
	}

	private static void renderCube(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2,
			float y2, float z2, float r, float g, float b, float a, int light) {

// Vorderseite (Z+)
		consumer.addVertex(pose, x1, y1, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, 1).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y1, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, 1).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, 1).setUv(0f,
				0f);
		consumer.addVertex(pose, x1, y2, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, 1).setUv(0f,
				0f);

// Rückseite (Z-)
		consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, -1).setUv(0f,
				0f);
		consumer.addVertex(pose, x1, y2, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, -1).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y2, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, -1).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y1, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 0, -1).setUv(0f,
				0f);

// Oben (Y+)
		consumer.addVertex(pose, x1, y2, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 1, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x1, y2, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 1, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 1, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y2, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, 1, 0).setUv(0f,
				0f);

// Unten (Y-)
		consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, -1, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y1, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, -1, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y1, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, -1, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x1, y1, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 0, -1, 0).setUv(0f,
				0f);

// Links (X-)
		consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, -1, 0, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x1, y1, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, -1, 0, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x1, y2, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, -1, 0, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x1, y2, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, -1, 0, 0).setUv(0f,
				0f);
// Rechts (X+)
		consumer.addVertex(pose, x2, y1, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 1, 0, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y2, z1).setColor(r, g, b, a).setLight(light).setNormal(pose, 1, 0, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 1, 0, 0).setUv(0f,
				0f);
		consumer.addVertex(pose, x2, y1, z2).setColor(r, g, b, a).setLight(light).setNormal(pose, 1, 0, 0).setUv(0f,
				0f);
	}
}