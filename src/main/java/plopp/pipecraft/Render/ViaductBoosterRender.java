package plopp.pipecraft.Render;

import java.util.Arrays;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductSpeed;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;

@EventBusSubscriber(value = Dist.CLIENT)
public class ViaductBoosterRender {

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
		Font font = mc.font;
		BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

		Direction[] allDirections = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

		for (BlockPos pos : BlockPos.betweenClosed(mc.player.blockPosition().offset(-16, -16, -16),
				mc.player.blockPosition().offset(16, 16, 16))) {

			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof BlockViaductSpeed))
				continue;

			int speed = state.getValue(BlockViaductSpeed.SPEED).getValue();
			String text = String.valueOf(speed);
			Direction blockFacing = state.getValue(BlockViaductSpeed.FACING);

			BlockEntity be = level.getBlockEntity(pos);
			ItemStack idStack = ItemStack.EMPTY;

			if (be instanceof BlockEntityViaductSpeed speedBE) {
				idStack = speedBE.getIdStack();
			}

			List<Direction> directionsToRender;
			switch (blockFacing) {
			case WEST, EAST ->
				directionsToRender = List.of(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN);
			case NORTH, SOUTH ->
				directionsToRender = List.of(Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);
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
					case WEST -> {
						zOffset += -0.07;
						yOffset += -0.07;
					}
					case EAST -> {
						zOffset += -0.07;
						yOffset += -0.07;
					}
					case NORTH -> {
						xOffset += -0.07;
						yOffset += -0.07;
					}
					case SOUTH -> {
						xOffset += -0.07;
						yOffset += -0.07;
					}
					case UP -> {
						if (blockFacing.getAxis() == Direction.Axis.Z)
							zOffset += -0.06;
						else
							xOffset += -0.06;
					}
					case DOWN -> {
						if (blockFacing.getAxis() == Direction.Axis.Z)
							zOffset += -0.06;
						else
							xOffset += -0.06;
					}
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
					if (blockFacing == Direction.WEST || blockFacing == Direction.EAST)
						poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
				}
				case DOWN -> {
					poseStack.mulPose(Axis.XP.rotationDegrees(-90));
					if (blockFacing == Direction.NORTH || blockFacing == Direction.SOUTH)
						poseStack.mulPose(Axis.ZP.rotationDegrees(180));
					if (blockFacing == Direction.WEST || blockFacing == Direction.EAST)
						poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
				}
				default -> {
					float rotation = switch (facing) {
					case NORTH -> 180f;
					case SOUTH -> 0f;
					case WEST -> -90f;
					case EAST -> 90f;
					default -> 0f;
					};
					poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
					poseStack.mulPose(Axis.XP.rotationDegrees(180));

					if (isHorizontal) {
						switch (facing) {
						case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
						case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
						case NORTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
						case SOUTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
						default -> throw new IllegalArgumentException("Unexpected value: " + facing);
						}
					}
				}
				}

				poseStack.pushPose();
				poseStack.scale(0.016f, 0.016f, 0.016f);
				font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFFFF, false, poseStack.last().pose(),
						bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
				poseStack.popPose();

				if (!idStack.isEmpty()) {
					poseStack.pushPose();
					poseStack.translate(-0.07, -0.05, 0.03);
					poseStack.mulPose(Axis.XP.rotationDegrees(180));

					if (idStack.getItem() instanceof BlockItem blockItem) {
						poseStack.scale(0.12f, 0.12f, 0.045f);
						Block blockToRender = blockItem.getBlock();
						dispatcher.renderSingleBlock(blockToRender.defaultBlockState(), poseStack, bufferSource,
								15728880, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
					} else {
						poseStack.scale(0.35f, 0.35f, 0.35f);
						poseStack.translate(0.16, 0.05, 0.09);
						mc.getItemRenderer().renderStatic(idStack, ItemDisplayContext.GROUND, 15728880,
								OverlayTexture.NO_OVERLAY, poseStack, bufferSource, mc.level, 0);
					}

					poseStack.popPose();
				}

				poseStack.popPose();
			}
		}
	}
}