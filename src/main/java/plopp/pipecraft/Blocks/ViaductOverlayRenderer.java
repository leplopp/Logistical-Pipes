package plopp.pipecraft.Blocks;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaduct;

public class ViaductOverlayRenderer implements BlockEntityRenderer<BlockEntityViaduct> {
	
	 public ViaductOverlayRenderer(BlockEntityRendererProvider.Context context) {
	        // optional: Zugriff auf RenderDispatcher, ModelManager, etc.
	    }

	 @Override
	 public void render(BlockEntityViaduct tile, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
	     poseStack.pushPose();
	     
        // Leicht anheben über das Modell
        poseStack.translate(0, 0.001, 0);

        TextureAtlasSprite sprite = Minecraft.getInstance()
        	    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
        	    .apply(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "block/viaducttest"));

        VertexConsumer builder = buffer.getBuffer(RenderType.CUTOUT_MIPPED);
        Matrix4f matrix = poseStack.last().pose();

        // Normale nach oben zeigend (x=0, y=1, z=0)
        Vector3f normal = new Vector3f(0, 1, 0);
        int alpha = 128;

        int lightU = packedLight & 0xFFFF;
        int lightV = (packedLight >> 16) & 0xFFFF;

        builder.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, alpha).setUv(sprite.getU1(), sprite.getV1()).setUv2(lightU, lightV).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, alpha).setUv(sprite.getU0(), sprite.getV1()).setUv2(lightU, lightV).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(matrix, 1, 0.5f, 1).setColor(255, 255, 255, alpha).setUv(sprite.getU0(), sprite.getV0()).setUv2(lightU, lightV).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(matrix, 0, 0.5f, 1).setColor(255, 255, 255, alpha).setUv(sprite.getU1(), sprite.getV0()).setUv2(lightU, lightV).setNormal(normal.x(), normal.y(), normal.z());

        poseStack.popPose();
    }
}


