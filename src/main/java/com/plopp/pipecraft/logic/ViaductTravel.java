package com.plopp.pipecraft.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
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

public class ViaductTravel {
	
	private static final Map<UUID, Integer> ticksPerChunkMap = new HashMap<>();
	private static final Map<UUID, Integer> tickCounters = new HashMap<>();
	public static final Map<UUID, List<BlockPos>> activeTravels = new HashMap<>();
    private static final Map<UUID, Integer> travelProgress = new HashMap<>();
    private static final Map<UUID, Double> progressMap = new HashMap<>();
    private static final Set<UUID> jumpAfterTravel = new HashSet<>();
    private static final Set<UUID> jumpTrigger = new HashSet<>();
    public static final int MAX_CHARGE = 30; //charge time
    public static final Map<UUID, List<ItemStack>> storedArmor = new HashMap<>();
    

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
    
    public static void start(Player player, BlockPos startPos, BlockPos targetPos, int ticksPerChunk) {
    	 System.out.println("[ViaductTravel] start() called with start=" + startPos + ", target=" + targetPos);
        Level level = player.level();

        List<BlockPos> path = findViaductPath(level, startPos, targetPos);

        if (!path.isEmpty()) {
            UUID id = player.getUUID();
            activeTravels.put(id, path);
            travelProgress.put(id, 0);
            tickCounters.put(id, 0);
            ticksPerChunkMap.put(id, ticksPerChunk);

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

        if (!player.level().isClientSide() && activeTravels.containsKey(id)) {
            List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(1.5));
            for (ItemEntity item : items) {
                item.setPickUpDelay(20);
            }
        }

        if (!activeTravels.containsKey(id)) return;

        List<BlockPos> path = activeTravels.get(id);
        int currentIndex = travelProgress.getOrDefault(id, 0);
        int lastIndex = path.size() - 1;
        if (currentIndex >= lastIndex) return;

        int ticksPerChunk = ticksPerChunkMap.getOrDefault(id, 1);
        if (ticksPerChunk <= 0) return;

        int tickCounter = tickCounters.getOrDefault(id, 0) + 1;
        tickCounters.put(id, tickCounter);

        double chunkProgress = progressMap.getOrDefault(id, 0.0);
        double chunkStep = 1.0 / ticksPerChunk;
        chunkProgress += chunkStep;

        int chunkSize = 16;
        int stepsLeft = Math.min(chunkSize, lastIndex - currentIndex);

        double totalStepProgress = chunkProgress * stepsLeft;
        int subIndex = (int) totalStepProgress;
        double lerpProgress = totalStepProgress - subIndex;

        int fromIndex = Math.min(currentIndex + subIndex, lastIndex - 1);
        int toIndex = Math.min(fromIndex + 1, lastIndex);

        Vec3 from = vecFromBlockPos(path.get(fromIndex));
        Vec3 to = vecFromBlockPos(path.get(toIndex));
        Vec3 lerped = from.lerp(to, lerpProgress);

        double lookAheadDistance = 0.1;
        double lookAheadProgress = lerpProgress + lookAheadDistance;
        int lookAheadSubIndex = subIndex;

        while (lookAheadProgress > 1.0) {
            lookAheadProgress -= 1.0;
            lookAheadSubIndex++;
        }

        int lookAheadFromIndex = Math.min(currentIndex + lookAheadSubIndex, lastIndex - 1);
        int lookAheadToIndex = Math.min(lookAheadFromIndex + 1, lastIndex);

        Vec3 lookFrom = vecFromBlockPos(path.get(lookAheadFromIndex));
        Vec3 lookTo = vecFromBlockPos(path.get(lookAheadToIndex));
        Vec3 lookTarget = lookFrom.lerp(lookTo, lookAheadProgress);

        Vec3 lookDirection = lookTarget.subtract(lerped).normalize();
        float yaw = (float) (Math.toDegrees(Math.atan2(lookDirection.z, lookDirection.x)) - 90);
        player.setYRot(yaw);

        double y = (toIndex == lastIndex && chunkProgress >= 1.0) ? to.y + 1.0 : lerped.y - 1;

        if (player instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            stop(player, false);
            return;
        } else {
            player.teleportTo(lerped.x, y, lerped.z);
        }

        if (chunkProgress >= 1.0) {
            travelProgress.put(id, currentIndex + stepsLeft);
            progressMap.put(id, 0.0);
        } else {
            progressMap.put(id, chunkProgress);
        }

        if (travelProgress.get(id) >= lastIndex) {
            BlockPos stonePos = path.get(lastIndex);
            player.teleportTo(stonePos.getX() + 0.5, stonePos.getY() + 1.0, stonePos.getZ() + 0.5);
            stop(player, true);
        }
    }
    
    public static List<BlockPos> findViaductPath(Level level, BlockPos start, BlockPos end) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<List<BlockPos>> queue = new LinkedList<>();

        queue.add(List.of(start));
        visited.add(start);

        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos last = path.get(path.size() - 1);

            if (last.equals(end)) {
                if (path.size() >= 3) {
                    return path;
                } else {
                    continue; 
                }
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = last.relative(dir);
                if (visited.contains(next)) continue;

                BlockState state = level.getBlockState(next);

                if (state.is(BlockRegister.VIADUCTLINKER) && !next.equals(end)) continue;

                if (state.getBlock() instanceof BlockViaduct || next.equals(end)) {
                    visited.add(next);
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        return List.of(); 
    }
    
    public static BlockPos findTarget(Level level, BlockPos from) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<List<BlockPos>> queue = new LinkedList<>();

        queue.add(List.of(from));
        visited.add(from);

        int maxDistance = 512; 
        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos last = path.get(path.size() - 1);

            if (!last.equals(from) && level.getBlockState(last).is(BlockRegister.VIADUCTLINKER) && path.size() >= 3) {
            	 BlockPos above = last.above();
                 BlockState aboveState = level.getBlockState(above);
                 if (!(aboveState.getBlock() instanceof BlockViaduct)) {
                return last.immutable();
            }else {
                continue;
            }
       }

            for (Direction dir : Direction.values()) {
                BlockPos next = last.relative(dir);
                if (visited.contains(next)) continue;

                if (from.distManhattan(next) > maxDistance) continue; 

                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof BlockViaduct || state.is(BlockRegister.VIADUCTLINKER)) {
                    visited.add(next);
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        return null;
    }
    
    public static void stop(Player player, boolean includeTeleport) {
        UUID id = player.getUUID();
      
        List<BlockPos> path = activeTravels.remove(id);
        travelProgress.remove(id);
        tickCounters.remove(id);
        
        List<ItemStack> armor = storedArmor.remove(player.getUUID());
        if (armor != null && armor.size() == 4) {
            player.getInventory().armor.set(3, armor.get(0)); // Helm
            player.getInventory().armor.set(2, armor.get(1)); // Brust
            player.getInventory().armor.set(1, armor.get(2)); // Hose
            player.getInventory().armor.set(0, armor.get(3)); // Schuhe
        }
        player.noPhysics = false;
        player.setPose(Pose.STANDING);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        markJumpAfterTravel(player);
        

        if (includeTeleport && path != null && !path.isEmpty()) {
            BlockPos stonePos = path.get(path.size() - 1);
            player.teleportTo(stonePos.getX() + 0.5, stonePos.getY() + 1.0, stonePos.getZ() + 0.5);
            BlockPos below = stonePos.below();                   // Block darunter
            BlockState belowState = player.level().getBlockState(below);

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
}