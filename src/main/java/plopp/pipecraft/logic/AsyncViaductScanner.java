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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.ViaductBlockRegistry;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductTeleporter;
import plopp.pipecraft.Network.data.DataEntryRecordTeleport;
import plopp.pipecraft.Network.linker.LinkedTargetEntry;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;
import plopp.pipecraft.logic.Manager.ViaductTeleporterManager;

public class AsyncViaductScanner {
	
    private final MinecraftServer server;
    private final ResourceKey<Level> startDimension;
    private final DimBlockPos startPos;
    private final Set<DimBlockPos> visited = new HashSet<>();
    private final Queue<DimBlockPos> toVisit = new ArrayDeque<>();
    private final List<LinkedTargetEntry> foundLinkers = new ArrayList<>();
    public final Map<DimBlockPos, DimBlockPos> cameFrom;
    private final int maxStepsPerTick;
    public final Map<DimBlockPos, Boolean> pathContainsTeleporters = new HashMap<>();
    public volatile boolean aborted;
    
    public AsyncViaductScanner(Level startLevel, BlockPos startPos, int maxStepsPerTick, Map<DimBlockPos, DimBlockPos> cameFrom) {
        this.server = startLevel.getServer();
        this.startDimension = startLevel.dimension();
        this.startPos = new DimBlockPos(startDimension, startPos);
        this.maxStepsPerTick = maxStepsPerTick;
        this.cameFrom = cameFrom;

        visited.add(this.startPos);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = startPos.relative(dir);
            DimBlockPos neighbor = new DimBlockPos(startDimension, neighborPos);
            if (visited.contains(neighbor)) continue;

            ServerLevel serverLevel = server.getLevel(startDimension);
            if (serverLevel == null) continue;

            BlockState neighborState = serverLevel.getBlockState(neighborPos);
            BlockEntity neighborBe = serverLevel.getBlockEntity(neighborPos);

            if (ViaductBlockRegistry.isViaduct(neighborState) ||
                neighborBe instanceof BlockEntityViaductLinker ||
                neighborBe instanceof BlockEntityViaductTeleporter) {
                toVisit.add(neighbor);
                cameFrom.put(neighbor, this.startPos); 
            }
        }
    }

    public boolean tick() {
        int steps = 0;
        if (aborted) return true;
        while (!toVisit.isEmpty() && steps < maxStepsPerTick) {
            if (aborted) return true;
            DimBlockPos current = toVisit.poll();

            if (!visited.add(current)) continue;

            ServerLevel currentLevel = server.getLevel(current.dimension);
            if (currentLevel == null) continue;

            BlockState currentState = currentLevel.getBlockState(current.pos);
            BlockEntity currentBe = currentLevel.getBlockEntity(current.pos);

            if (currentBe instanceof BlockEntityViaductTeleporter teleporter) {
                pathContainsTeleporters.put(current, true);

                DimBlockPos targetDimPos = new DimBlockPos(current.dimension, current.pos);
                pathContainsTeleporters.put(targetDimPos, true);
        	    
                ItemStack goalIcon = teleporter.getTargetDisplayedItem();
                if (!goalIcon.isEmpty()) {
                    String goalKey = BlockEntityViaductTeleporter.generateItemId(goalIcon);
                    
                    outerLevelLoop:
                    for (ServerLevel level : server.getAllLevels()) {
                        @SuppressWarnings("unused")
						ResourceKey<Level> dim = level.dimension();

                        for (Map.Entry<DimBlockPos, TeleporterEntryRecord> entry : ViaductTeleporterManager.getAll().entrySet()) {
                            DimBlockPos dimPos = entry.getKey();
                            if (visited.contains(dimPos)) continue;

                            DataEntryRecordTeleport startEntry = entry.getValue().start();
                            String startKey = BlockEntityViaductTeleporter.generateItemId(startEntry.icon());

                            if (startKey.equals(goalKey)) {
                                visited.add(dimPos);
                                ServerLevel targetLevel = server.getLevel(dimPos.getDimension());
                                if (targetLevel == null) continue;

                                for (Direction dir : Direction.values()) {
                                    BlockPos neighborPos = dimPos.getPos().relative(dir);
                                    DimBlockPos neighbor = new DimBlockPos(dimPos.getDimension(), neighborPos);
                                    if (visited.contains(neighbor)) continue;

                                    BlockEntity neighborBe = targetLevel.getBlockEntity(neighborPos);
                                    BlockState neighborState = targetLevel.getBlockState(neighborPos);
                                    if (neighborBe instanceof BlockEntityViaductTeleporter
                                    	    || neighborBe instanceof BlockEntityViaductLinker
                                    	    || ViaductBlockRegistry.isViaduct(neighborState)) {

                                    	    if (!visited.contains(neighbor) && !toVisit.contains(neighbor)) {
                                    	        toVisit.add(neighbor);
                                    	        cameFrom.put(neighbor, current);
                                    	    }
                                    	}
                                }           
                                break outerLevelLoop; 
                            }
                        }
                    }
                }
                continue;
            }

            if (!ViaductBlockRegistry.isViaduct(currentState)) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.pos.relative(dir);
                DimBlockPos neighbor = new DimBlockPos(current.dimension, neighborPos);

                if (visited.contains(neighbor) || toVisit.contains(neighbor)) continue;

                ServerLevel currentServerLevel = server.getLevel(current.dimension);
                if (currentServerLevel == null) continue;

                BlockState neighborState = currentServerLevel.getBlockState(neighborPos);
                BlockEntity neighborBe = currentServerLevel.getBlockEntity(neighborPos);

                if ((neighborBe instanceof BlockEntityViaductLinker || neighborBe instanceof BlockEntityViaductTeleporter) && !neighbor.pos.equals(startPos.pos)) {
                    if (ViaductBlockRegistry.areViaductBlocksConnected(currentServerLevel, current.pos, neighborPos)) {
                        if (neighborBe instanceof BlockEntityViaductLinker linker) {
                            foundLinkers.add(new LinkedTargetEntry(neighborPos, linker.getCustomName()));
                        }
                        toVisit.add(neighbor);
                        cameFrom.put(neighbor, current); 
                    }
                } else if (ViaductBlockRegistry.isViaduct(neighborState)) {
                    if (ViaductBlockRegistry.areViaductBlocksConnected(currentServerLevel, current.pos, neighborPos)) {
                    	if (visited.contains(neighbor) || toVisit.contains(neighbor)) continue;
                        toVisit.add(neighbor);
                        cameFrom.put(neighbor, current); 
                    }
                }
            }

            steps++;
        }

        return toVisit.isEmpty();
    }
    
    public boolean hasTeleporterOnPath(List<DimBlockPos> path) {
        for (DimBlockPos dimPos : path) {
            ServerLevel level = server.getLevel(dimPos.getDimension());
            if (level == null) continue;

            BlockEntity be = level.getBlockEntity(dimPos.getPos());
            if (be instanceof BlockEntityViaductTeleporter) {
                return true;
            }
        }
        return false;
    }
    
    public List<LinkedTargetEntry> getFoundLinkers() {
        return foundLinkers;
    }
    
    public DimBlockPos getDimPosFor(LinkedTargetEntry entry) {
        for (DimBlockPos visitedPos : visited) {
            if (visitedPos.pos.equals(entry.getPos())) {
                return visitedPos;
            }
        }
        return new DimBlockPos(startDimension, entry.getPos()); 
    }
    
    public List<DimBlockPos> constructDimPath(DimBlockPos end) {
        List<DimBlockPos> path = new ArrayList<>();
        DimBlockPos current = end;

        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }

        Collections.reverse(path);
        return path;
    }
    
    public DimBlockPos getTargetDimPos() {
        if (foundLinkers.isEmpty()) return null;

        LinkedTargetEntry best = foundLinkers.get(0);
        for (DimBlockPos visitedPos : visited) {
            if (visitedPos.pos.equals(best.getPos())) {
                return visitedPos;
            }
        }
        return new DimBlockPos(startDimension, best.getPos());
    }
}