package plopp.pipecraft.logic.pipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.Pipes.BlockPipe;

public class PipeTravel {

    private static final List<PipeTravelEntry> activeItems = new ArrayList<>();
    private static final Set<BlockPos> pipePositions = new HashSet<>();

    public static void addPipe(BlockPos pos) {
        pipePositions.add(pos);
    }

    public static void removePipe(BlockPos pos) {
        pipePositions.remove(pos);
    }
    
    public static void tick(ServerLevel level) {
    	for (BlockPos pipePos : pipePositions) {
    	    BlockState pipeState = level.getBlockState(pipePos);
    	    if (!(pipeState.getBlock() instanceof BlockPipe)) continue;

    	    for (Direction dir : Direction.values()) {
    	        BlockPos neighbor = pipePos.relative(dir);
    	        BlockEntity be = level.getBlockEntity(neighbor);
    	        if (be instanceof Container container) {
    	            for (int i = 0; i < container.getContainerSize(); i++) {
    	                ItemStack stack = container.getItem(i);
    	                if (!stack.isEmpty()) {
    	                    activeItems.add(new PipeTravelEntry(stack.copy(), pipePos, dir));
    	                    // container.setItem(i, ItemStack.EMPTY); <-- NICHT löschen
    	                    break; // nur 1 Stack pro Container pro Tick
    	                }
    	            }
    	        }
    	    }
    	}

    	Iterator<PipeTravelEntry> iterator = activeItems.iterator();
    	while (iterator.hasNext()) {
    	    PipeTravelEntry entry = iterator.next();
    	    entry.progress += 10;

    	    if (entry.progress < 100) continue;

    	    BlockPos currentPos = entry.currentPos;
    	    BlockState currentState = level.getBlockState(currentPos);
    	    Block currentBlock = currentState.getBlock();

    	    if (!(currentBlock instanceof BlockPipe pipe)) {
    	        // Fallback: spawne Item
    	        spawnItemEntity(level, currentPos, entry.stack);
    	        iterator.remove();
    	        continue;
    	    }

    	    // Nächste Richtung berechnen (nicht zurück)
    	    Direction nextDir = getNextPipeDirection(currentState, entry.movingDir.getOpposite(), pipe, entry.movingDir);
    	    BlockPos nextPos = currentPos.relative(nextDir);
    	    BlockState nextState = level.getBlockState(nextPos);
    	    BlockEntity nextBe = level.getBlockEntity(nextPos);

    	    if (nextState.getBlock() instanceof BlockPipe) {
    	        // Weiterleiten in Pipe
    	        entry.currentPos = nextPos;
    	        entry.movingDir = nextDir;
    	        entry.progress = 0;
    	    } else if (nextBe instanceof Container container) {
    	        // Container aufnehmen
    	        ItemStack remaining = tryInsert(container, entry.stack);
    	        if (!remaining.isEmpty()) spawnItemEntity(level, nextPos, remaining);
    	        iterator.remove();
    	    } else {
    	        // Dead-End → ItemEntity spawnen
    	        spawnItemEntity(level, nextPos, entry.stack);
    	        iterator.remove();
    	    }
    	}

        // Neue Items aus Containern ziehen
        for (BlockPos pipePos : pipePositions) {
            BlockState pipeState = level.getBlockState(pipePos);
            if (!(pipeState.getBlock() instanceof BlockPipe)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pipePos.relative(dir);
                BlockEntity be = level.getBlockEntity(neighbor);
                if (be instanceof Container container) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack stack = container.getItem(i);
                        if (!stack.isEmpty()) {
                            // **Items werden aus der Chest entfernt**
                            activeItems.add(new PipeTravelEntry(stack.copy(), pipePos, dir));
                            container.setItem(i, ItemStack.EMPTY); 
                            break; // nur 1 Stack pro Tick
                        }
                    }
                }
            }
        }
    }

    private static void spawnItemEntity(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                stack);
        level.addFreshEntity(entity);
    }

    private static Direction getNextPipeDirection(BlockState state, Direction fromDir, BlockPipe pipe, Direction fallback) {
        for (Direction dir : Direction.values()) {
            if (dir == fromDir) continue; // nicht zurück
            Boolean connected = state.getOptionalValue(pipe.getPropertyForDirection(dir)).orElse(false);
            if (connected) return dir;
        }
        return fallback; // Wenn keine andere Richtung, weiter in Start-Richtung
    }

    private static ItemStack tryInsert(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack.copy());
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItem(slot, stack) && slot.getCount() + stack.getCount() <= slot.getMaxStackSize()) {
                slot.grow(stack.getCount());
                container.setItem(i, slot);
                return ItemStack.EMPTY;
            }
        }
        return stack; // bleibt übrig, wenn nicht alles eingefügt werden konnte
    }

    public static void startFromChest(ServerLevel level, BlockPos chestPos) {
        BlockEntity be = level.getBlockEntity(chestPos);
        if (!(be instanceof Container container)) return;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                for (Direction dir : Direction.values()) {
                    BlockPos pipePos = chestPos.relative(dir);
                    BlockState pipeState = level.getBlockState(pipePos);
                    if (pipeState.getBlock() instanceof BlockPipe) {
                        // NEU: erst PipeTravelEntry hinzufügen, dann Chest nicht sofort leeren
                        activeItems.add(new PipeTravelEntry(stack.copy(), pipePos, dir));

                        // Chest löschen passiert erst im nächsten Tick, wenn PipeTravelEntry erfolgreich hinzugefügt wurde
                        container.setItem(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }
        }
    }
}