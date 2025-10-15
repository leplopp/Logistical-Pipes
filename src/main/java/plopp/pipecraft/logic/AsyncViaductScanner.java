package plopp.pipecraft.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.ViaductBlockRegistry;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductTeleporter;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.Network.linker.LinkedTargetEntry;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterManager;

public class AsyncViaductScanner {
	
    private final MinecraftServer server;
    private final ResourceKey<Level> startDimension;
    private final DimBlockPos startPos;
    private final Set<DimBlockPos> visited = new HashSet<>();
    private final Queue<DimBlockPos> toVisit = new ArrayDeque<>();
    private final List<LinkedTargetEntry> foundLinkers = new ArrayList<>();
    private final Map<DimBlockPos, DimBlockPos> cameFrom;
    private final int maxStepsPerTick;

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

        while (!toVisit.isEmpty() && steps < maxStepsPerTick) {
            DimBlockPos current = toVisit.poll();

            if (!visited.add(current)) continue;

            ServerLevel currentLevel = server.getLevel(current.dimension);
            if (currentLevel == null) continue;

            BlockState currentState = currentLevel.getBlockState(current.pos);
            BlockEntity currentBe = currentLevel.getBlockEntity(current.pos);
            PipeCraftIndex.LOGGER.info("Scan current position: {} mit BlockEntity: {}", current, currentBe);

            if (currentBe instanceof BlockEntityViaductTeleporter teleporter) {
                PipeCraftIndex.LOGGER.info("Scanne Teleporter an Position {}", current);

                ItemStack goalIcon = teleporter.getTargetDisplayedItem();
                if (!goalIcon.isEmpty()) {

                    for (ServerLevel level : server.getAllLevels()) {
                        ResourceKey<Level> dim = level.dimension();

                        for (Map.Entry<BlockPos, TeleporterEntryRecord> entry : ViaductTeleporterManager.getAll().entrySet()) {
                            BlockPos possibleTargetPos = entry.getKey();

                            DimBlockPos dimPos = new DimBlockPos(dim, possibleTargetPos);
                            if (visited.contains(dimPos)) continue;

                            DataEntryRecord startEntry = entry.getValue().start();

                            if (ItemStack.isSameItem(startEntry.icon(), goalIcon)) {

                                ServerLevel targetLevel = server.getLevel(dim);
                                if (targetLevel == null) continue;

                                for (Direction dir : Direction.values()) {
                                    BlockPos neighborPos = possibleTargetPos.relative(dir);
                                    DimBlockPos neighbor = new DimBlockPos(dim, neighborPos);
                                    if (visited.contains(neighbor)) continue;

                                    BlockEntity neighborBe = targetLevel.getBlockEntity(neighborPos);
                                    BlockState neighborState = targetLevel.getBlockState(neighborPos);

                                    if (neighborBe instanceof BlockEntityViaductTeleporter
                                        || neighborBe instanceof BlockEntityViaductLinker
                                        || ViaductBlockRegistry.isViaduct(neighborState)) {
                                        toVisit.add(neighbor);
                                    }
                                }

                                visited.add(dimPos);
                                break; 
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
                        toVisit.add(neighbor);
                        cameFrom.put(neighbor, current); 
                    }
                }
            }

            steps++;
        }

        return toVisit.isEmpty();
    }

    public List<LinkedTargetEntry> getFoundLinkers() {
        return foundLinkers;
    }
    
    public List<BlockPos> constructPath(DimBlockPos end) {
        List<BlockPos> path = new ArrayList<>();
        DimBlockPos current = end;
        while (current != null) {
            path.add(current.pos);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }
    
    public List<BlockPos> getResult() {
        if (foundLinkers.isEmpty()) return null;

        LinkedTargetEntry best = foundLinkers.get(0); 
        DimBlockPos target = new DimBlockPos(startDimension, best.getPos());
        
        return constructPath(target);
    }
    public DimBlockPos findFirstTeleporterInPath(List<BlockPos> path) {
        ServerLevel level = server.getLevel(startDimension);
        if (level == null) return null;

        for (BlockPos pos : path) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlockEntityViaductTeleporter) {
                return new DimBlockPos(startDimension, pos);
            }
        }
        return null;
    }
}