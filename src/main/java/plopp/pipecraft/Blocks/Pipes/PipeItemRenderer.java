package plopp.pipecraft.Blocks.Pipes;

import java.util.List;
import java.util.Objects;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
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
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = mc.renderBuffers().bufferSource();
        float partialTicks = event.getPartialTick().getRealtimeDeltaTicks();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        List<TravellingItem> snapshot;
        synchronized (PipeTravel.activeItems) {
            snapshot = PipeTravel.activeItems.stream()
                .filter(Objects::nonNull)
                .toList();
        }

        for (TravellingItem item : snapshot) {
        	if (item.stack.isEmpty() || !(item.stack.getItem() instanceof BlockItem)) continue;

            Vec3 from = Vec3.atCenterOf(item.lastPos);
            Vec3 to = Vec3.atCenterOf(item.currentPos);
            double progress = Mth.clamp(item.progress + partialTicks * item.speed, 0.0, 1.0);
            Vec3 itemPos = from.lerp(to, progress);

            poseStack.pushPose();
            poseStack.translate(itemPos.x - cameraPos.x, itemPos.y - cameraPos.y - 0.16, itemPos.z - cameraPos.z);

            int light = LevelRenderer.getLightColor(mc.level, item.currentPos);

            mc.getItemRenderer().renderStatic(
                    item.stack,
                    ItemDisplayContext.GROUND,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    mc.level,
                    0
            );

            poseStack.popPose();
        }

        ((BufferSource) buffer).endBatch();
    }
}