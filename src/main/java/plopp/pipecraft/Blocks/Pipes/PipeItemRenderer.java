package plopp.pipecraft.Blocks.Pipes;

import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import plopp.pipecraft.logic.pipe.PipeTravel;
import plopp.pipecraft.logic.pipe.TravellingItem;

@EventBusSubscriber(value = Dist.CLIENT)
public class PipeItemRenderer {

	 @SubscribeEvent
	    public static void onRenderLevelStage(RenderLevelStageEvent event) {
	        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;

	        Minecraft mc = Minecraft.getInstance();
	        PoseStack ms = event.getPoseStack();
	        MultiBufferSource buffer = mc.renderBuffers().bufferSource();
	        float partial = event.getPartialTick().getRealtimeDeltaTicks();

	        for (TravellingItem item : new ArrayList<>(PipeTravel.activeItems)) {
	          
	            if (item.stack.isEmpty()) continue;

	            Vec3 from = Vec3.atCenterOf(item.lastPos);
	            Vec3 to = Vec3.atCenterOf(item.currentPos);
	            double interp = Math.min(1.0, Math.max(0.0, item.progress + partial * item.speed));
	            Vec3 itemPos = from.lerp(to, interp);

	            Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
	            ms.pushPose();
	            ms.translate(itemPos.x - camPos.x, itemPos.y -0.16 - camPos.y, itemPos.z - camPos.z);

	            int light = LevelRenderer.getLightColor(mc.level, item.currentPos);

	            mc.getItemRenderer().renderStatic(item.stack, ItemDisplayContext.GROUND, light,OverlayTexture.NO_OVERLAY, ms, buffer, mc.level, 0);
	            ms.popPose();
	        }

	        ((BufferSource) buffer).endBatch();
    }
}