package plopp.pipecraft.logic;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.ViaductBlockRegistry;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.travel.ClientTravelDataManager;
import plopp.pipecraft.Network.travel.PacketTravelStop;
import plopp.pipecraft.Network.travel.TravelStatePacket;

public class ViaductTravel {
	
	private static final Map<UUID, Float> travelPitchMap = new HashMap<>();
	public static final Map<UUID, TravelData> activeTravels = new HashMap<>();
    private static final Set<UUID> jumpAfterTravel = new HashSet<>();
    private static final Set<UUID> jumpTrigger = new HashSet<>();
    private static final Set<UUID> resetModelSet = new HashSet<>();
    private static final Map<UUID, Float> travelYawMap = new HashMap<>();
    private static final Map<UUID, VerticalDirection> verticalDirMap = new HashMap<>();
    public enum VerticalDirection {NONE, UP, DOWN}
    
    public static void markResetModel(UUID id) {
    resetModelSet.add(id);
	}

	public static boolean consumeResetModel(UUID id) {
		
    return resetModelSet.remove(id);
	}
    
    public static void markJumpAfterTravel(Player player) {
        jumpAfterTravel.add(player.getUUID());
    }

    public static boolean shouldJumpAfterTravel(Player player) {
        return jumpAfterTravel.contains(player.getUUID());
    }

    public static void clearJumpFlag(Player player) {
        jumpAfterTravel.remove(player.getUUID());
    }
    public static boolean shouldTriggerJump(UUID id) {
        return jumpTrigger.remove(id); 
    }
    
    private static Vec3 vecFromBlockPos(BlockPos pos) {
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
    

   private static float getYawFromDirection(Direction dir) {
	    return switch (dir) {
	        case NORTH -> 180f;
	        case SOUTH -> 0f;
	        case WEST  -> 90f;
	        case EAST  -> -90f;
	        default    -> 0f;
	    };
	}

	private static float getPitchFromDirection(Direction dir) {
	    return switch (dir) {
	        case UP   -> 90f;
	        case DOWN -> -90f;
	        default   -> 0f;
	    };
	}

	private static VerticalDirection getVerticalDirection(Direction dir) {
	    return switch (dir) {
	        case UP   -> VerticalDirection.UP;
	        case DOWN -> VerticalDirection.DOWN;
	        default   -> VerticalDirection.NONE;
	    };
	}
	
	public static void start(Player player, BlockPos startPos, BlockPos targetPos, int ticksPerChunk) {
	    Level level = player.level();
	    UUID uuid = player.getUUID();

	    TravelData data = new TravelData(level, startPos, targetPos, ticksPerChunk, false);
	    data.finalTargetPos = targetPos;

	    BlockEntity startBE = level.getBlockEntity(startPos);
	    if (startBE instanceof BlockEntityViaductLinker linker) {
	        linker.asyncScanner = null;
	        linker.setAsyncScanInProgress(false);

	        List<BlockPos> pathToTarget = linker.scannedPaths.get(targetPos);
	        if (pathToTarget != null && !pathToTarget.isEmpty()) {
	            data.path = pathToTarget;
	            data.phase = TravelData.TravelPhase.TO_FINAL_TARGET;
	            data.hasTeleporterPhase = false;
	        } else {
	            stop(player, true);
	            return;
	        }
	    }

	    activeTravels.put(uuid, data);

	    if (!level.isClientSide()) {
	        player.setInvulnerable(true);
	        player.setShiftKeyDown(false);
	        player.noPhysics = true;
	        player.setNoGravity(true);
	        player.setDeltaMovement(Vec3.ZERO);
	        player.setPose(Pose.SWIMMING);

	    }

	    if (player instanceof ServerPlayer serverPlayer) {
	        NetworkHandler.sendTravelStateToAll(serverPlayer, false);
	    }

	    for (Direction dir : Direction.values()) {
	        BlockPos candidate = startPos.relative(dir);
	        BlockState state = level.getBlockState(candidate);
	        if (ViaductBlockRegistry.isViaduct(state)) {
	            Vec3 start = vecFromBlockPos(candidate);
	            player.teleportTo(start.x, start.y - 1, start.z);

	            float yaw = getYawFromDirection(dir);
	            float pitch = getPitchFromDirection(dir);

	            player.setYRot(yaw);
	            player.setXRot(pitch);

	            data.lockedX = start.x;
	            data.lockedY = start.y - 1;
	            data.lockedZ = start.z;

	            travelYawMap.put(uuid, yaw);
	            travelPitchMap.put(uuid, pitch);
	            verticalDirMap.put(uuid, getVerticalDirection(dir));
	            return;
	        }
	    }

	    Vec3 fallback = new Vec3(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
	    player.teleportTo(fallback.x, fallback.y, fallback.z);
	}
   
	public static void tick(Player player) {
	    TravelData data = activeTravels.get(player.getUUID());
	    if (data == null) return;

	    if (!player.level().isClientSide()) {
	        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(1.5));
	        for (ItemEntity item : items) {
	            item.setPickUpDelay(20);
	        }
	    }

	    List<BlockPos> path = data.path;
	    if (path == null || path.isEmpty()) return;

	    int currentIndex = data.progressIndex;
	    int lastIndex = path.size() - 1;
	    if (currentIndex >= lastIndex || data.ticksPerChunk <= 0) return;

	    data.tickCounter++;
	    data.chunkProgress += (1.0 / data.ticksPerChunk);

	    int stepsLeft = Math.min(16, lastIndex - currentIndex);
	    double totalStepProgress = data.chunkProgress * stepsLeft;
	    int subIndex = (int) totalStepProgress;
	    double lerpProgress = totalStepProgress - subIndex;

	    int fromIndex = Math.min(currentIndex + subIndex, lastIndex - 1);
	    int toIndex = Math.min(fromIndex + 1, lastIndex);

	    for (int i = currentIndex; i <= fromIndex; i++) {
	        BlockState state = player.level().getBlockState(path.get(i));
	        if (state.getBlock() instanceof BlockViaductSpeed speedBlock) {
	            data.ticksPerChunk = speedBlock.getSpeed(state);
	        }
	    }

	    Vec3 from = vecFromBlockPos(path.get(fromIndex));
	    Vec3 to = vecFromBlockPos(path.get(toIndex));
	    Vec3 lerped = from.lerp(to, lerpProgress);

	    BlockState fromState = player.level().getBlockState(path.get(fromIndex));
	    if (fromState.getBlock() instanceof BlockViaductSpeed speedBlock) {
	        data.ticksPerChunk = speedBlock.getSpeed(fromState);
	    }

	    double lookAheadProgress = lerpProgress + 0.1;
	    int lookAheadSubIndex = subIndex;
	    while (lookAheadProgress > 1.0) {
	        lookAheadProgress -= 1.0;
	        lookAheadSubIndex++;
	    }

	    int lookFromIndex = Math.min(currentIndex + lookAheadSubIndex, lastIndex - 1);
	    int lookToIndex = Math.min(lookFromIndex + 1, lastIndex);
	    Vec3 lookFrom = vecFromBlockPos(path.get(lookFromIndex));
	    Vec3 lookTo = vecFromBlockPos(path.get(lookToIndex));
	    Vec3 lookTarget = lookFrom.lerp(lookTo, lookAheadProgress);

	    updatePlayerDirection(player, lerped, lookTarget);

	    if (player instanceof ServerPlayer sp) {
	        NetworkHandler.sendTravelStateToAll(sp, false);
	    }

	    if (player instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
	        stop(player, false);
	        return;
	    }

	    player.teleportTo(lerped.x, lerped.y - 0, lerped.z);

	    if (data.chunkProgress >= 1.0) {
	        data.progressIndex += stepsLeft;
	        data.chunkProgress = 0.0;
	    }

	    if (data.progressIndex >= lastIndex) {
	        if (data.phase == TravelData.TravelPhase.TO_START_TELEPORTER) {
	            BlockPos startTeleporterPos = path.get(lastIndex);
	            BlockEntity be = player.level().getBlockEntity(startTeleporterPos);
	            if (be instanceof BlockEntityViaductLinker linker) {
	                List<BlockPos> pathToFinalTarget = linker.scannedPaths.get(data.finalTargetPos);
	                if (pathToFinalTarget != null && !pathToFinalTarget.isEmpty()) {
	                    data.path = pathToFinalTarget;
	                    data.progressIndex = 0;
	                    data.chunkProgress = 0.0;
	                    data.phase = TravelData.TravelPhase.TO_FINAL_TARGET;
	                    return; 
	                }
	            }
	            stop(player, true);
	        } else {
	            stop(player, true);
	        }
	    }
	}
	
	public static void updatePlayerDirection(Player player, Vec3 lerped, Vec3 lookTarget) {
	    Vec3 lookDirection = lookTarget.subtract(lerped).normalize();

	    float pitch = (float) Math.toDegrees(Math.asin(lookDirection.y));
	    VerticalDirection vdir = VerticalDirection.NONE;
	    double verticalThreshold = 0.8;
	    if (lookDirection.y > verticalThreshold) vdir = VerticalDirection.UP;
	    else if (lookDirection.y < -verticalThreshold) vdir = VerticalDirection.DOWN;

	    float yaw; // nur einmal

	    if (vdir == VerticalDirection.NONE) {
	        // normale horizontale Rotation
	        yaw = (float) Math.toDegrees(Math.atan2(-lookDirection.x, lookDirection.z));
	        if (yaw < 0) yaw += 360.0f;
	    } else {
	        // vertikal: letzten Yaw nehmen, oder horizontalen Unterschied falls vorhanden
	        TravelData data = activeTravels.get(player.getUUID());
	        if (data != null && data.progressIndex > 0) {
	            Vec3 prev = vecFromBlockPos(data.path.get(Math.max(0, data.progressIndex - 1)));
	            Vec3 curr = lerped;

	            double dx = curr.x - prev.x;
	            double dz = curr.z - prev.z;

	            if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
	                yaw = travelYawMap.getOrDefault(player.getUUID(), 0f);
	            } else {
	                yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
	                if (yaw < 0) yaw += 360;
	            }
	        } else {
	            yaw = travelYawMap.getOrDefault(player.getUUID(), 0f);
	        }
	    }

	    // Speichern
	    travelYawMap.put(player.getUUID(), yaw);
	    travelPitchMap.put(player.getUUID(), pitch);
	    verticalDirMap.put(player.getUUID(), vdir);

	    player.setYRot(yaw);
	    player.setXRot(pitch);
	}

    public static BlockPos findTarget(Level level, BlockPos from) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (!current.equals(from) && level.getBlockState(current).is(BlockRegister.VIADUCTLINKER)) {
                BlockPos above = current.above();
                BlockState aboveState = level.getBlockState(above);

                if (!(aboveState.getBlock() instanceof BlockViaduct)) {
                    return current.immutable();
                } else {
                    continue;
                }
            }

            BlockState currentState = level.getBlockState(current);
            boolean currentIsLinker = currentState.is(BlockRegister.VIADUCTLINKER);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);
                boolean neighborIsLinker = neighborState.is(BlockRegister.VIADUCTLINKER);

                if (neighborIsLinker && currentIsLinker) {
                    if (!ViaductBlockRegistry.isViaduct(neighborState)) {
                        continue; 
                    }
                }

                if (ViaductBlockRegistry.isViaduct(neighborState) || neighborIsLinker) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }
    
    public static void resume(Player player) {
        UUID id = player.getUUID();
        TravelData data = activeTravels.get(id);
        if (data == null) return;

        player.setInvulnerable(true);
        player.setSwimming(false);
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
    }

  
    public static void stop(Player player, boolean includeTeleport) {
        UUID id = player.getUUID();

        travelYawMap.remove(id);
        verticalDirMap.remove(id);
        ViaductTravel.markResetModel(id);

        TravelData data = activeTravels.remove(id);
        List<BlockPos> path = data != null ? data.path : null;

        player.noPhysics = false;
        player.setPose(Pose.STANDING);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        
        markJumpAfterTravel(player);
        
        if (player instanceof ServerPlayer sp) {
            NetworkHandler.sendTravelStateToAll(sp, true);
            NetworkHandler.sendToClient(sp, new PacketTravelStop(sp.getUUID()));
        }

        if (includeTeleport && path != null && !path.isEmpty()) {
            BlockPos stonePos = path.get(path.size() - 1);
            Level level = player.level();
            BlockState targetState = level.getBlockState(stonePos);

            Vec3 teleportPos;

            if (targetState.is(BlockRegister.VIADUCTLINKER)) {
                Direction facing = targetState.getValue(BlockViaductLinker.FACING);

                double x = stonePos.getX() + 0.5;
                double z = stonePos.getZ() + 0.5;
                double y;

                if (facing.getAxis().isHorizontal()) {

                    double offsetAmount = 1;
                    switch (facing) {
                        case NORTH -> z -= offsetAmount;
                        case SOUTH -> z += offsetAmount;
                        case WEST -> x -= offsetAmount;
                        case EAST -> x += offsetAmount;
					default -> throw new IllegalArgumentException("Unexpected value: " + facing);
                    }

                    BlockPos checkPos = stonePos.relative(facing);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (!checkState.isAir()) {
                        y = stonePos.getY() + 1.1;
                    } else {
                        y = stonePos.getY() + 0.1;
                    }
                } else if (facing == Direction.UP) {
   
                    BlockPos checkPos = stonePos.above();
                    BlockState checkState = level.getBlockState(checkPos);

                    x = stonePos.getX() + 0.5;
                    z = stonePos.getZ() + 0.5;

                    if (!checkState.isAir()) {
                        y = stonePos.getY() + 2.1; 
                    } else {
                        y = stonePos.getY() + 1.1; 
                    }
                } else if (facing == Direction.DOWN) {

                    BlockPos checkPos = stonePos.below();
                    BlockState checkState = level.getBlockState(checkPos);

                    x = stonePos.getX() + 0.5;
                    z = stonePos.getZ() + 0.5;

                    if (!checkState.isAir()) {
                        y = stonePos.getY() - 2.9;
                    } else {
                        y = stonePos.getY() - 0.9; 
                    }
                } else {
                    y = stonePos.getY() + 1.0;
                }

                teleportPos = new Vec3(x, y, z);
            } else {
                teleportPos = new Vec3(stonePos.getX() + 0.5, stonePos.getY(), stonePos.getZ() + 0.5);
            }

            player.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);

            if (targetState.is(BlockRegister.VIADUCTLINKER)) {
                Direction facing = targetState.getValue(BlockViaductLinker.FACING);

                if (facing == Direction.UP) {
                    if (player.level().isClientSide() && player instanceof LocalPlayer localPlayer) {
                        localPlayer.jumpFromGround();
                    } else {
                        jumpTrigger.add(player.getUUID());
                    }
                }
            }
        }
        
        if (player instanceof ServerPlayer serverPlayer) {
        	NetworkHandler.sendTravelStateToAll(serverPlayer, true);
            
        }
    }
}