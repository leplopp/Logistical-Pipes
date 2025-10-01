package plopp.pipecraft.logic.pipe;

import java.util.ArrayList;
import java.util.Collections;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.Blocks.Pipes.BlockPipe;
import plopp.pipecraft.Blocks.Pipes.BlockPipeExtract;

public class PipeTravel {
	public static final List<TravellingItem> activeItems = new ArrayList<>();

    public static void tick(Level level) {
        for (TravellingItem item : new ArrayList<>(activeItems)) {
            item.tick(level);
        }
    }


    public static void insertItem(ItemStack stack, BlockPos startContainer, Direction side, ServerLevel level, PipeConfig config) {
        TravellingItem item = new TravellingItem(stack, startContainer, side, config, level);
        item.fromContainer = true; 
        item.justExtracted = true;
        activeItems.add(item);
    }

    public static void finishItem(TravellingItem item, ServerLevel level) {
        BlockEntity be = level.getBlockEntity(item.currentPos);
        // Falls ein Container existiert
        if (be instanceof Container container) {
            // Item kurz animiert Richtung Container-Ende (optional)
            item.progress = 1;

            ItemStack leftover = item.stack.copy();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) {
                    container.setItem(i, leftover);
                    leftover = ItemStack.EMPTY;
                    break;
                } else if (ItemStack.isSameItem(slot, leftover) &&
                           slot.getCount() + leftover.getCount() <= slot.getMaxStackSize()) {
                    slot.grow(leftover.getCount());
                    container.setItem(i, slot);
                    leftover = ItemStack.EMPTY;
                    break;
                }
            }

            // Alles, was nicht mehr passt, rausploppen
            if (!leftover.isEmpty()) spawnItemEntity(level, item.currentPos, leftover);
        } else {
            // Kein Container → Item ploppt raus
            spawnItemEntity(level, item.currentPos, item.stack);
        }

        activeItems.remove(item);
    }

    public static void spawnItemEntity(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level,pos.getX() + 0.5,pos.getY() + 0.5,pos.getZ() + 0.5,stack);
        level.addFreshEntity(entity);
    }

    public static Target getNextTarget(TravellingItem item, BlockPos current, BlockPos lastPos, Direction side) {
        ServerLevel level = item.level;
        BlockEntity beHere = level.getBlockEntity(current);
        BlockState currentState = level.getBlockState(current);

        boolean isExtractorPipe = currentState.getBlock() instanceof BlockPipeExtract;

        // === Inventar am aktuellen Block ===
        IItemHandler hereInv = level.getCapability(Capabilities.ItemHandler.BLOCK, current, side);
        if (hereInv != null) {
            if (!item.justExtracted) {
                // Item kommt gerade aus Pipe → muss zuerst in Inventar
                return new Target(current, side, false, true);
            }


            // Item startet frisch aus Inventar → Extractor prüfen
            List<Target> extractors = new ArrayList<>();
            for (Direction d : Direction.values()) {
                BlockPos candidate = current.relative(d);
                if (!level.hasChunkAt(candidate)) continue;
                BlockState state = level.getBlockState(candidate);
                if (state.getBlock() instanceof BlockPipeExtract) {
                    extractors.add(new Target(candidate, d, false, false));
                }
            }

            if (!extractors.isEmpty()) {
                int index = TravellingItem.containerExtractorIndex.getOrDefault(current, 0);
                Target chosen = extractors.get(index % extractors.size());
                TravellingItem.containerExtractorIndex.put(current, (index + 1) % extractors.size());
                item.justExtracted = false;
                return chosen;
            }

            item.justExtracted = false;
            return new Target(current, side, false, true);
        }

        // === Pipes und Inventare sammeln ===
        List<Target> possibleTargets = new ArrayList<>();
        for (Direction d : Direction.values()) {
            BlockPos candidate = current.relative(d);
            if (candidate.equals(lastPos)) continue;
            if (!level.hasChunkAt(candidate)) continue;

            BlockState state = level.getBlockState(candidate);
            BlockEntity be = level.getBlockEntity(candidate);

            // Normale Pipes immer als Ziel
            if (state.getBlock() instanceof BlockPipe && !(state.getBlock() instanceof BlockPipeExtract)) {
                possibleTargets.add(new Target(candidate, d, false, false));
            }
            else if (!isExtractorPipe) {
                IItemHandler inv = level.getCapability(Capabilities.ItemHandler.BLOCK, candidate, d.getOpposite());
                if (inv != null) {
                    possibleTargets.add(new Target(candidate, d, true, false));
                }
            }
        }

        if (!possibleTargets.isEmpty()) {
            int index = TravellingItem.pipeDirectionIndex.getOrDefault(current, 0);
            Target chosen = possibleTargets.get(index % possibleTargets.size());
            TravellingItem.pipeDirectionIndex.put(current, (index + 1) % possibleTargets.size());
            return chosen;
        }

        // === Extraktor-Pipes prüfen, falls Item aus Container kommt ===
        if (item.fromContainer) {
            for (Direction d : Direction.values()) {
                BlockPos candidate = current.relative(d);
                if (candidate.equals(lastPos)) continue;
                if (!level.hasChunkAt(candidate)) continue;
                BlockState state = level.getBlockState(candidate);
                if (state.getBlock() instanceof BlockPipeExtract) {
                    return new Target(candidate, d, false, false);
                }
            }
        }

        return null; // Kein Ziel
    }
}