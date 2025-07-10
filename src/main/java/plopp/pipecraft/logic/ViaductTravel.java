package plopp.pipecraft.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;

public class ViaductTravel {
	
	private static final Map<UUID, Float> travelPitchMap = new HashMap<>();
	public static final Map<UUID, TravelData> activeTravels = new HashMap<>();
    private static final Set<UUID> jumpAfterTravel = new HashSet<>();
    private static final Set<UUID> jumpTrigger = new HashSet<>();
    public static final int MAX_CHARGE = 30; //charge time
    private static final Set<UUID> resetModelSet = new HashSet<>();
    public static final Map<UUID, List<ItemStack>> storedArmor = new HashMap<>();
    private static final Map<UUID, Float> travelYawMap = new HashMap<>();
    private static final Map<UUID, VerticalDirection> verticalDirMap = new HashMap<>();
    public enum VerticalDirection {
        NONE, UP, DOWN
    }
    
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
        return activeTravels.containsKey(player.getUUID());
        
    }
    
    public static float getTravelPitch(UUID uuid) {
        return travelPitchMap.getOrDefault(uuid, 0f);
    }
    
    public static void start(Player player, BlockPos startPos, BlockPos targetPos, int ticksPerChunk) {
        System.out.println("[ViaductTravel] start() called with start=" + startPos + ", target=" + targetPos);
        Level level = player.level();

        List<BlockPos> path = findViaductPath(level, startPos, targetPos);

        if (!path.isEmpty()) {
            UUID id = player.getUUID();
            activeTravels.put(id, new TravelData(path, ticksPerChunk));

            if (!player.level().isClientSide()) {
                player.setInvulnerable(true);
                player.setShiftKeyDown(false);
                player.noPhysics = true;
                player.setNoGravity(true);
                player.setDeltaMovement(Vec3.ZERO);

                ItemStack helmet = player.getInventory().armor.get(3);
                ItemStack chestplate = player.getInventory().armor.get(2);
                ItemStack leggings = player.getInventory().armor.get(1);
                ItemStack boots = player.getInventory().armor.get(0);

                storedArmor.put(player.getUUID(), List.of(helmet, chestplate, leggings, boots));
                player.getInventory().armor.set(3, ItemStack.EMPTY);
                player.getInventory().armor.set(2, ItemStack.EMPTY);
                player.getInventory().armor.set(1, ItemStack.EMPTY);
                player.getInventory().armor.set(0, ItemStack.EMPTY);
            }
        }
    }
  
    public static void tick(Player player) {
        UUID id = player.getUUID();
        TravelData data = activeTravels.get(id);
        if (data == null) return;

        if (!player.level().isClientSide()) {
            List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(1.5));
            for (ItemEntity item : items) {
                item.setPickUpDelay(20);
            }
        }

        List<BlockPos> path = data.path;
        int currentIndex = data.progressIndex;
        int lastIndex = path.size() - 1;
        if (currentIndex >= lastIndex) return;

        if (data.ticksPerChunk <= 0) return;

        data.tickCounter++;
        data.chunkProgress += (1.0 / data.ticksPerChunk);

        int chunkSize = 16;
        int stepsLeft = Math.min(chunkSize, lastIndex - currentIndex);

        double totalStepProgress = data.chunkProgress * stepsLeft;
        int subIndex = (int) totalStepProgress;
        double lerpProgress = totalStepProgress - subIndex;

        int fromIndex = Math.min(currentIndex + subIndex, lastIndex - 1);
        int toIndex = Math.min(fromIndex + 1, lastIndex);

        Vec3 from = vecFromBlockPos(path.get(fromIndex));
        Vec3 to = vecFromBlockPos(path.get(toIndex));
        Vec3 lerped = from.lerp(to, lerpProgress);

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

        double y = (toIndex == lastIndex && data.chunkProgress >= 1.0) ? to.y + 1.0 : lerped.y - 1;

        if (player instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            stop(player, false);
            return;
        } else {
            player.teleportTo(lerped.x, y, lerped.z);
        }

        if (data.chunkProgress >= 1.0) {
            data.progressIndex += stepsLeft;
            data.chunkProgress = 0.0;
        }

        if (data.progressIndex >= lastIndex) {
            BlockPos stonePos = path.get(lastIndex);
            player.teleportTo(stonePos.getX() + 0.5, stonePos.getY() + 1.0, stonePos.getZ() + 0.5);
            stop(player, true);
        }
    }
    
    public static void updatePlayerDirection(Player player, Vec3 lerped, Vec3 lookTarget) {
    	Vec3 lookDirection = lookTarget.subtract(lerped).normalize();

    	float yaw = (float) Math.toDegrees(Math.atan2(-lookDirection.x, lookDirection.z));
    	if (yaw < 0) yaw += 360.0f;
    	yaw = yaw % 360;

    	float pitch = (float) Math.toDegrees(Math.asin(lookDirection.y)); 

    	travelYawMap.put(player.getUUID(), yaw);
    	travelPitchMap.put(player.getUUID(), pitch);

    	player.setYRot(yaw);

    	double verticalThreshold = 0.8;
    	VerticalDirection dir = VerticalDirection.NONE;
    	if (lookDirection.y > verticalThreshold) {
    	    dir = VerticalDirection.UP;
    	} else if (lookDirection.y < -verticalThreshold) {
    	    dir = VerticalDirection.DOWN;
    	}
    	verticalDirMap.put(player.getUUID(), dir);
    }

    public static float getTravelYaw(UUID uuid) {
        return travelYawMap.getOrDefault(uuid, 0f);
    }

    public static VerticalDirection getVerticalDirection(UUID uuid) {
        return verticalDirMap.getOrDefault(uuid, VerticalDirection.NONE);
    }
    
    public static List<BlockPos> findViaductPath(Level level, BlockPos start, BlockPos end) {
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (current.equals(end)) {

                List<BlockPos> path = new ArrayList<>();
                BlockPos step = current;
                while (step != null) {
                    path.add(step);
                    step = cameFrom.get(step);
                }
                Collections.reverse(path);

                if (path.size() >= 3) {
                    return path;
                } else {
                    continue;
                }
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;

                BlockState state = level.getBlockState(neighbor);

                if (state.is(BlockRegister.VIADUCTLINKER) && !neighbor.equals(end)) continue;

                if (state.getBlock() instanceof BlockViaduct || neighbor.equals(end)) {
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return List.of();
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

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;

                BlockState state = level.getBlockState(neighbor);
                if (state.getBlock() instanceof BlockViaduct || state.is(BlockRegister.VIADUCTLINKER)) {
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
        ViaductTravel.markResetModel(player.getUUID());
        player.refreshDimensions();

        TravelData data = activeTravels.remove(id);
        List<BlockPos> path = data != null ? data.path : null;

        List<ItemStack> armor = storedArmor.remove(player.getUUID());

        if (armor != null && armor.size() == 4) {
            player.getInventory().armor.set(3, armor.get(0)); 
            player.getInventory().armor.set(2, armor.get(1)); 
            player.getInventory().armor.set(1, armor.get(2)); 
            player.getInventory().armor.set(0, armor.get(3)); 
        }

        player.noPhysics = false;
        player.setPose(Pose.STANDING);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        markJumpAfterTravel(player);

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

            BlockPos below = stonePos.below();                  
            BlockState belowState = level.getBlockState(below);

            if (belowState.getBlock() instanceof BlockViaduct) {
                System.out.println("===> SPRINGE! Viaduct unter Stone bei " + below);

                if (player.level().isClientSide() && player instanceof LocalPlayer) {
                    ((LocalPlayer) player).jumpFromGround();
                } else {
                    jumpTrigger.add(player.getUUID());
                }
            }
        }
    }
  
    public static class TravelData {
        public final List<BlockPos> path;
        public int progressIndex = 0;
        public double chunkProgress = 0.0;
        public int ticksPerChunk;
        public int tickCounter = 0;

        public TravelData(List<BlockPos> path, int ticksPerChunk) {
            this.path = path;
            this.ticksPerChunk = ticksPerChunk;
        }
    }
}