package plopp.pipecraft.Blocks.Pipes;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import plopp.pipecraft.ClientConfig;
import plopp.pipecraft.logic.pipe.PipeTravel;
import plopp.pipecraft.logic.pipe.TravellingItem;

@EventBusSubscriber(value = Dist.CLIENT)
public class PipeItemRenderer {

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		  if (event.getStage() != ClientConfig.getRenderStage())
		        return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		float partialTicks = event.getPartialTick().getRealtimeDeltaTicks();

		MultiBufferSource buffer = mc.renderBuffers().bufferSource();

		synchronized (PipeTravel.activeItems) {
			for (TravellingItem item : List.copyOf(PipeTravel.activeItems)) {
				if (item == null || item.stack.isEmpty())
					continue;

				Vec3 from = Vec3.atCenterOf(item.lastPos);
				Vec3 to = Vec3.atCenterOf(item.currentPos);
				double progress = Mth.clamp(item.progress + partialTicks * item.speed, 0.0, 1.0);
				Vec3 itemPos = from.lerp(to, progress);
				double maxDistSqr = ClientConfig.getItemRenderDistanceBlocks()
						* ClientConfig.getItemRenderDistanceBlocks();
				if (itemPos.distanceToSqr(cameraPos) > maxDistSqr)
					continue;

				poseStack.pushPose();
				poseStack.translate(itemPos.x - cameraPos.x, itemPos.y - cameraPos.y - 0.15, itemPos.z - cameraPos.z);

				int light = LevelRenderer.getLightColor(mc.level, item.currentPos);

				mc.getItemRenderer().renderStatic(item.stack, ItemDisplayContext.GROUND, light,
						OverlayTexture.NO_OVERLAY, poseStack, buffer, mc.level, 0);

				poseStack.popPose();
			}
		}
	}
}