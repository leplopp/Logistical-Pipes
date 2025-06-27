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
import com.plopp.pipecraft.Blocks.Viaduct.BlockViaduct;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ViaductTravel {
	
	private static final Map<UUID, Integer> ticksPerChunkMap = new HashMap<>();
	private static final Map<UUID, Integer> ticksPerBlockMap = new HashMap<>();
	private static final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> activeTravels = new HashMap<>();
    private static final Map<UUID, Integer> travelProgress = new HashMap<>();
    private static final Map<UUID, Double> progressMap = new HashMap<>();
    private static final Set<UUID> jumpAfterTravel = new HashSet<>();
    private static final Set<UUID> jumpTrigger = new HashSet<>();

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
        return jumpTrigger.remove(id); // gibt true zurück und entfernt es gleichzeitig
    }
    
    public static void start(Player player, BlockPos startPos, int ticksPerBlock, int ticksPerChunk) {
        Level level = player.level();
        BlockPos target = findTarget(level, startPos);

        if (target != null) {
            List<BlockPos> path = findViaductPath(level, startPos, target);
            if (!path.isEmpty()) {
                UUID id = player.getUUID();
                activeTravels.put(id, path);
                travelProgress.put(id, 0);
                tickCounters.put(id, 0);
                ticksPerBlockMap.put(id, ticksPerBlock);
                ticksPerChunkMap.put(id, ticksPerChunk);

                if (!player.level().isClientSide()) {
                    player.setInvisible(true);
                    player.setInvulnerable(true);
                    player.setSwimming(false);
                    player.setPose(Pose.STANDING);
                    player.setShiftKeyDown(false);
                    player.noPhysics = true;
                    player.setNoGravity(true);
                }
            }
        }
    }

    public static void tick(Player player) {
        UUID id = player.getUUID();

        if (!activeTravels.containsKey(id)) return;

        int ticksPerBlock = ticksPerBlockMap.getOrDefault(id, 0);
        int ticksPerChunk = ticksPerChunkMap.getOrDefault(id, 0);
        int tickCounter = tickCounters.getOrDefault(id, 0);

        tickCounter++;
        tickCounters.put(id, tickCounter);

        List<BlockPos> path = activeTravels.get(id);
        int index = travelProgress.getOrDefault(id, 0);

        if (index >= path.size() - 1) {
            stop(player);
            return;
        }

        if (ticksPerBlock > 0 && ticksPerChunk == 0) {
            // Blockmodus (bisheriger Modus)
            if (tickCounter < ticksPerBlock) return;
            tickCounters.put(id, 0);

            index++;
            travelProgress.put(id, index);

            BlockPos targetPos = path.get(index);
            boolean isTargetBlock = (index == path.size() - 1);
            teleportPlayerTo(player, targetPos, isTargetBlock);

        } else if (ticksPerChunk > 0 && ticksPerBlock == 0) {
            double chunkProgress = progressMap.getOrDefault(id, 0.0);
            double chunkStep = 1.0 / ticksPerChunk;
            chunkProgress += chunkStep;

            int chunkSize = 16;
            int currentIndex = travelProgress.getOrDefault(id, 0);

            if (currentIndex >= path.size() - 1) {
                stop(player);
                return;
            }

            int lastIndex = path.size() - 1;
            int stepsLeft = Math.min(chunkSize, lastIndex - currentIndex);

            // Aktueller Fortschritt durch die 16 Schritte
            double totalStepProgress = chunkProgress * stepsLeft;

            int subIndex = (int) totalStepProgress;
            double lerpProgress = totalStepProgress - subIndex;

            int fromIndex = Math.min(currentIndex + subIndex, path.size() - 2);
            int toIndex = Math.min(fromIndex + 1, path.size() - 1);
            
            if (fromIndex >= path.size() - 1 || toIndex >= path.size()) {
                stop(player);
                return;
            }
            Vec3 from = vecFromBlockPos(path.get(fromIndex));
            Vec3 to = vecFromBlockPos(path.get(toIndex));
            Vec3 lerped = from.lerp(to, lerpProgress);

            double y;
            if (toIndex == lastIndex && chunkProgress >= 1.0) {
                y = to.y + 1.0;
            } else {
                y = lerped.y - 1.0;
            }

            player.teleportTo(lerped.x, y, lerped.z);

            // Fortschritt abschließen
            if (chunkProgress >= 1.0) {
                travelProgress.put(id, currentIndex + stepsLeft);
                chunkProgress = 0.0;
            }

            progressMap.put(id, chunkProgress);
        }
    }
    private static Vec3 vecFromBlockPos(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    private static void teleportPlayerTo(Player player, BlockPos pos, boolean isTargetBlock) {
        double x = pos.getX() + 0.5;
        double y = isTargetBlock ? pos.getY() + 1.0 : pos.getY() - 1.0;
        double z = pos.getZ() + 0.5;
        player.teleportTo(x, y, z);
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
                // Erlaube nur Pfade mit mindestens einem Viaduct dazwischen
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

                // STONE nur als Zielblock erlaubt, nicht zwischendrin
                if (state.is(Blocks.STONE) && !next.equals(end)) continue;

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
    
    public static boolean isTravelActive(Player player) {
        return activeTravels.containsKey(player.getUUID());
        
    }
    private static BlockPos findTarget(Level level, BlockPos from) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<List<BlockPos>> queue = new LinkedList<>();

        queue.add(List.of(from));
        visited.add(from);

        int maxDistance = 512; 
        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos last = path.get(path.size() - 1);

            // Ziel gefunden (aber nicht direkt neben Start!)
            if (!last.equals(from) && level.getBlockState(last).is(Blocks.STONE) && path.size() >= 3) {
            	 BlockPos above = last.above();
                 BlockState aboveState = level.getBlockState(above);
                 if (!(aboveState.getBlock() instanceof BlockViaduct)) {
                return last.immutable();
            }else {
                // Ziel ignorieren, weil oben Viaduct ist
                continue;
            }
       }

            for (Direction dir : Direction.values()) {
                BlockPos next = last.relative(dir);
                if (visited.contains(next)) continue;

                if (from.distManhattan(next) > maxDistance) continue; // Sicherheitsgrenze

                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof BlockViaduct || state.is(Blocks.STONE)) {
                    visited.add(next);
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        return null;
    }
    
    public static void stop(Player player) {
        UUID id = player.getUUID();
        List<BlockPos> path = activeTravels.remove(id);
        travelProgress.remove(id);
        ticksPerBlockMap.remove(id);
        tickCounters.remove(id);

        player.noPhysics = false;
        player.setPose(Pose.STANDING);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        markJumpAfterTravel(player);

        if (path != null && !path.isEmpty()) {
            BlockPos stonePos = path.get(path.size() - 1);
            player.teleportTo(stonePos.getX() + 0.5, stonePos.getY() + 1.0, stonePos.getZ() + 0.5);
            BlockPos below = stonePos.below();                   // Block darunter
            BlockState belowState = player.level().getBlockState(below);

            if (belowState.getBlock() instanceof BlockViaduct) {
                System.out.println("===> SPRINGE! Viaduct unter Stone bei " + below);

                if (player.level().isClientSide() && player instanceof LocalPlayer) {
                    // Für direkte Clients (optional)
                    ((LocalPlayer) player).jumpFromGround();
                } else {
                    // Für Server: Setze Flag, das Client auswertet
                    jumpTrigger.add(player.getUUID());
                }
               }
            }
        }
}