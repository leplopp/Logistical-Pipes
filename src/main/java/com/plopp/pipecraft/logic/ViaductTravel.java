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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ViaductTravel {
    private static final Map<UUID, List<BlockPos>> activeTravels = new HashMap<>();
    private static final Map<UUID, Integer> travelProgress = new HashMap<>();


    
    public static void start(Player player, BlockPos startPos) {
        Level level = player.level();
        BlockPos target = findTarget(level, startPos);

        if (target != null) {
            List<BlockPos> path = findViaductPath(level, startPos, target);
            if (!path.isEmpty()) {
                UUID id = player.getUUID();
                activeTravels.put(id, path);
                travelProgress.put(id, 0);

                if (!player.level().isClientSide()) {
                    player.setInvisible(true);
                }
            }
        }
    }


    public static void tick(Player player) {
        UUID id = player.getUUID();
        if (!activeTravels.containsKey(id)) return;

        if (player.xxa != 0 || player.zza != 0) {
            activeTravels.remove(id);
            travelProgress.remove(id);
            // Pose zurücksetzen, falls Fahrt abgebrochen
            player.setPose(Pose.STANDING);
            return;
        }

        List<BlockPos> path = activeTravels.get(id);
        int index = travelProgress.getOrDefault(id, 0);

        if (index >= path.size()) {
            stop(player);
            return;
        }

        BlockPos targetPos = path.get(index);
        double x = targetPos.getX() + 0.5;

        // Unterschiedliche Höhe während der Fahrt und am Ende:
        double y;
        if (index == path.size() - 1) {
            // Letzter Block (Cobblestone), auf dem Block stehen
            y = targetPos.getY() + 1.0;
        } else {
            // Während der Fahrt in der "Pipe"
            y = targetPos.getY() + -1;
        }

        double z = targetPos.getZ() + 0.5;
        
        // Teleportieren
        player.teleportTo(x, y, z);

        // Nächster Punkt
        travelProgress.put(id, index + 1);
    }
    
    public static List<BlockPos> findViaductPath(Level level, BlockPos start, BlockPos end) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<List<BlockPos>> queue = new LinkedList<>();

        queue.add(List.of(start));
        visited.add(start);

        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos last = path.get(path.size() - 1);

            // Pfad gefunden
            if (last.equals(end)) {
                // Erlaube nur Pfade mit mindestens einem Viaduct dazwischen
                if (path.size() >= 3) {
                    return path;
                } else {
                    continue; // Pfad zu kurz → überspringen
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

        return List.of(); // Kein Pfad gefunden
    }
    public static boolean isTravelActive(Player player) {
        return activeTravels.containsKey(player.getUUID());
    }
    private static BlockPos findTarget(Level level, BlockPos from) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<List<BlockPos>> queue = new LinkedList<>();

        queue.add(List.of(from));
        visited.add(from);

        int maxDistance = 512; // Optionaler Schutz
        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos last = path.get(path.size() - 1);

            // Ziel gefunden (aber nicht direkt neben Start!)
            if (!last.equals(from) && level.getBlockState(last).is(Blocks.STONE) && path.size() >= 3) {
                return last.immutable();
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
        activeTravels.remove(id);
        travelProgress.remove(id);

        if (!player.level().isClientSide()) {
            player.setPose(Pose.STANDING);
            player.setInvisible(false);
        }
    }
    
    
    
}