package plopp.pipecraft.logic.Travel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductTeleporter;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.travel.TravelStatePacket;
import plopp.pipecraft.logic.DimBlockPos;
import plopp.pipecraft.logic.Manager.ClientTravelDataManager;

public class ViaductTravel {
	public static final Map<UUID, Float> travelPitchMap = new HashMap<>();
	public static final Map<UUID, TravelData> activeTravels = new HashMap<>();// könnte problem sein guck mal resourcekey oder so später mal
	public static final Set<UUID> resetModelSet = new HashSet<>();
	public static final Map<UUID, Float> travelYawMap = new HashMap<>();
	public static final Map<UUID, VerticalDirection> verticalDirMap = new HashMap<>();

	public enum VerticalDirection {
		NONE, UP, DOWN
	}

	public static void markResetModel(UUID id) {
		resetModelSet.add(id);
	}

	public static boolean consumeResetModel(UUID id) {
		return resetModelSet.remove(id);
	}

	public static Vec3 vecFromBlockPos(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	public static Vec3 vecFromDimBlockPos(DimBlockPos dimPos) {
		BlockPos pos = dimPos.getPos();
		return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	public static boolean isTravelActive(Player player) {
		if (player.level().isClientSide()) {
			TravelStatePacket travelData = ClientTravelDataManager.getTravelData(player.getUUID());
			return travelData != null && travelData.isActive();
		} else {
			return activeTravels.containsKey(player.getUUID());
		}
	}

	public static float getTravelYaw(UUID uuid, Level level) {
		if (level != null && level.isClientSide()) {
			TravelStatePacket travelData = ClientTravelDataManager.getTravelData(uuid);
			return travelData != null ? travelData.getTravelYaw() : 0f;
		} else {
			return travelYawMap.getOrDefault(uuid, 0f);
		}
	}

	public static float getTravelPitch(UUID uuid, Level level) {
		if (level != null && level.isClientSide()) {
			TravelStatePacket travelData = ClientTravelDataManager.getTravelData(uuid);
			return travelData != null ? travelData.getTravelPitch() : 0f;
		} else {
			return travelPitchMap.getOrDefault(uuid, 0f);
		}
	}

	public static VerticalDirection getVerticalDirection(UUID uuid, Level level) {
		if (level != null && level.isClientSide()) {
			TravelStatePacket travelData = ClientTravelDataManager.getTravelData(uuid);
			return travelData != null ? travelData.getVerticalDirection() : VerticalDirection.NONE;
		} else {
			return verticalDirMap.getOrDefault(uuid, VerticalDirection.NONE);
		}
	}

	public static TravelData getTravelData(Player player) {
		return activeTravels.get(player.getUUID());
	}

	public static void tick(Player player) {
	    TravelData data = activeTravels.get(player.getUUID());
	    if (data == null) return;

	    if (data.isPaused)
	        return;

	    if (!player.level().isClientSide()) {
	        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class,
	                player.getBoundingBox().inflate(1.5));
	        for (ItemEntity item : items)
	            item.setPickUpDelay(20);
	    }


		List<DimBlockPos> path = data.path;
		if (path == null || path.isEmpty())
			return;

		int currentIndex = data.progressIndex;
		int lastIndex = path.size() - 1;
		if (currentIndex >= lastIndex || data.ticksPerChunk <= 0)
			return;

		Vec3 from = vecFromDimBlockPos(path.get(currentIndex));
		Vec3 to = vecFromDimBlockPos(path.get(Math.min(currentIndex + 1, lastIndex)));

		int ticksThisTick = data.ticksPerChunk;

		int safetyRange;
		if (ticksThisTick < 9) {
			safetyRange = 2;
		} else if (ticksThisTick < 15) {
			safetyRange = 1;
		} else {
			safetyRange = 0;
		}

		BlockPos min = new BlockPos((int) Math.floor(Math.min(from.x, to.x) - safetyRange),
				(int) Math.floor(Math.min(from.y, to.y) - safetyRange),
				(int) Math.floor(Math.min(from.z, to.z) - safetyRange));
		BlockPos max = new BlockPos((int) Math.floor(Math.max(from.x, to.x) + safetyRange),
				(int) Math.floor(Math.max(from.y, to.y) + safetyRange),
				(int) Math.floor(Math.max(from.z, to.z) + safetyRange));

		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = player.level().getBlockState(pos);

			if (state.getBlock() instanceof BlockViaductSpeed speedBlock) {
				ticksThisTick = speedBlock.getSpeed(state);
				data.ticksPerChunk = ticksThisTick;
			}

			if (state.getBlock() instanceof BlockViaductDetector detectorBlock) {
				detectorBlock.trigger(player.level(), pos, state);
			}

			  if (state.getBlock() instanceof BlockViaductTeleporter teleporterBlock) {
			        DimBlockPos dimPos = new DimBlockPos(player.level().dimension(), pos);

			        // Nur auslösen, wenn noch nicht getriggert
			        if (!data.triggeredTeleporters.contains(dimPos)) {
			            teleporterBlock.trigger(player.level(), pos);
			            data.triggeredTeleporters.add(dimPos);
			        }
			    }
		}

		data.chunkProgress += 16.0 / ticksThisTick;
		Vec3 lerped = from.lerp(to, data.chunkProgress);

		player.teleportTo(lerped.x, lerped.y, lerped.z);

		while (data.chunkProgress >= 1) {
			data.chunkProgress -= 1;
			data.progressIndex++;

			if (data.progressIndex >= lastIndex) {
				if (!data.isLastPhase()) {
					data.currentPhase++;

					data.path = new ArrayList<>(data.pathPhase.get(data.currentPhase));
					data.progressIndex = 0;
					data.chunkProgress = 0.0;
					path = data.path;
					lastIndex = path.size() - 1;
				} else {
					data.progressIndex = lastIndex;
					data.chunkProgress = 0.0;
					break;
				}
			}
		}

		Vec3 lookTarget;
		if (data.progressIndex >= lastIndex) {
			Vec3 prev = vecFromDimBlockPos(path.get(Math.max(lastIndex - 1, 0)));
			Vec3 last = vecFromDimBlockPos(path.get(lastIndex));
			lookTarget = last.add(last.subtract(prev));
		} else {
			int nextIndex = Math.min(data.progressIndex + 2, lastIndex);
			lookTarget = vecFromDimBlockPos(path.get(nextIndex));
		}

		updatePlayerDirection(player, lerped, lookTarget);

		if (player instanceof ServerPlayer sp) {
			NetworkHandler.sendTravelStateToAll(sp, false);
			if (sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
				TravelStop.stop(player, false);
				return;
			}
		}

		if (data.progressIndex >= lastIndex && data.isLastPhase()) {
			System.out.println("[VIADUCT DEBUG] Travel finished successfully!");
			TravelStop.stop(player, true);
		}
	}

	public static void updatePlayerDirection(Player player, Vec3 lerped, Vec3 lookTarget) {
		Vec3 lookDirection = lookTarget.subtract(lerped).normalize();

		float pitch = (float) Math.toDegrees(Math.asin(lookDirection.y));
		VerticalDirection vdir = VerticalDirection.NONE;
		double verticalThreshold = 0.2;
		if (lookDirection.y > verticalThreshold)
			vdir = VerticalDirection.UP;
		else if (lookDirection.y < -verticalThreshold)
			vdir = VerticalDirection.DOWN;

		float yaw;

		if (vdir == VerticalDirection.NONE) {
			yaw = (float) Math.toDegrees(Math.atan2(-lookDirection.x, lookDirection.z));
			if (yaw < 0)
				yaw += 360.0f;
		} else {
			TravelData data = activeTravels.get(player.getUUID());
			if (data != null && data.progressIndex > 0) {
				Vec3 prev = vecFromDimBlockPos(data.path.get(Math.max(0, data.progressIndex - 1)));
				Vec3 curr = lerped;

				double dx = curr.x - prev.x;
				double dz = curr.z - prev.z;

				if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
					yaw = travelYawMap.getOrDefault(player.getUUID(), 0f);
				} else {
					yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
					if (yaw < 0)
						yaw += 360;
				}
			} else {
				yaw = travelYawMap.getOrDefault(player.getUUID(), 0f);
			}
		}

		travelYawMap.put(player.getUUID(), yaw);
		travelPitchMap.put(player.getUUID(), pitch);
		verticalDirMap.put(player.getUUID(), vdir);

		player.setYRot(yaw);
		player.setXRot(pitch);
	}
}