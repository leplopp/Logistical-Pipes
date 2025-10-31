package plopp.pipecraft.Render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import plopp.pipecraft.Blocks.Facade.BlockFacadeTileEntity;

@OnlyIn(Dist.CLIENT)
public class FacadeTileEntityRenderer implements BlockEntityRenderer<BlockFacadeTileEntity> {

    public FacadeTileEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BlockFacadeTileEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState facade = be.getOriginalBlockState();
        if (facade != null && facade != Blocks.AIR.defaultBlockState()) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(facade, poseStack, buffer, packedLight, packedOverlay, null, null);
        }
    }
}