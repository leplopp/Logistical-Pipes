package plopp.pipecraft.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import plopp.pipecraft.Config;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.speeder.SpeedChangePacket;
import plopp.pipecraft.logic.ViaductTravel;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, value = Dist.CLIENT)
public class ClientRuntimeEvents {

	private static int lostSightTicks = 0;
	private static int tickCounter = 0;

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null)
			return;

		Player player = mc.player;

		if (BlockViaductSpeed.editingActive) {
			tickCounter++;

			if (tickCounter >= 60) {
				tickCounter = 0;
				mc.player.displayClientMessage(Component.translatable("viaduct.speed.change.start"), true);
			}
		} else {
			tickCounter = 0;
		}

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

			if (mc.options.getCameraType().isFirstPerson()) {
				float currentYaw = player.getYRot();
				float targetYaw = ViaductTravel.getTravelYaw(player.getUUID(), player.level());
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

	private static float lerpAngle(float from, float to, float alpha) {
		float difference = ((to - from + 540) % 360) - 180;
		return (from + alpha * difference) % 360;
	}

	@SubscribeEvent
	public static void onRenderPlayerPre(RenderLivingEvent.Pre<?, ?> event) {
		if (!(event.getEntity() instanceof AbstractClientPlayer player))
			return;
		// if (!(event.getRenderer() instanceof PlayerRenderer renderer)) return;
		if (!ViaductTravel.isTravelActive(player))
			return;

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

		if (!BlockViaductSpeed.editingActive || BlockViaductSpeed.editingPos == null || level == null || player == null)
			return;

		BlockState state = level.getBlockState(BlockViaductSpeed.editingPos);

		if (!(state.getBlock() instanceof BlockViaductSpeed) || !state.hasProperty(BlockViaductSpeed.SPEED))
			return;

		int scrollStep = (int) Math.signum(event.getScrollDeltaY());
		if (scrollStep == 0)
			return;

		if (player.isShiftKeyDown()) {
			scrollStep *= 10;
		}

		int currentSpeed = state.getValue(BlockViaductSpeed.SPEED).getValue();
		// int visualSpeed = 129 - currentSpeed;
		int newSpeed = currentSpeed + scrollStep;
		newSpeed = Mth.clamp(newSpeed, Config.getSpeedViaductMin(), Config.getSpeedViaductMax());
		level.playSound(player, BlockViaductSpeed.editingPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3f, 3.0f);
		int delta = newSpeed - currentSpeed;
		if (delta != 0) {
			SpeedChangePacket pkt = new SpeedChangePacket(BlockViaductSpeed.editingPos, delta);
			NetworkHandler.sendToServer(pkt);
		}

		event.setCanceled(true);
	}
}