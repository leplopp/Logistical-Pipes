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
        activeItems.add(item);
    }

    public static void finishItem(TravellingItem item, ServerLevel level) {
        BlockEntity be = level.getBlockEntity(item.currentPos);
        System.out.println("FinishItem called!");
        System.out.println("currentPos=" + item.currentPos);
        System.out.println("BlockState=" + level.getBlockState(item.currentPos));
        System.out.println("BlockEntity=" + level.getBlockEntity(item.currentPos));
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
            System.out.println("FinishItem @ " + item.currentPos + " -> " + be);
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

        // 1) Item im Container → nur Extractor-Pipes
        BlockEntity beHere = level.getBlockEntity(current);
        if (beHere instanceof Container && !current.equals(lastPos)) {
            if (!item.fromContainer) {
                return new Target(current, side, false, true);
            } else {
                // Alle angrenzenden Extractor-Pipes sammeln
                List<Direction> extractors = new ArrayList<>();
                for (Direction d : Direction.values()) {
                    BlockPos candidate = current.relative(d);
                    if (!level.hasChunkAt(candidate)) continue;

                    BlockState state = level.getBlockState(candidate);
                    Block block = state.getBlock();
                    if (block instanceof BlockPipeExtract) {
                        extractors.add(d);
                    }
                }

                if (extractors.isEmpty()) {
                    return new Target(current, side, false, true); // kein Extractor → bleibt
                }

                // Round-Robin: nächsten Extractor auswählen
                int nextIndex = (item.lastExtractorIndex + 1) % extractors.size();
                Direction chosenDir = extractors.get(nextIndex);
                item.lastExtractorIndex = nextIndex;

                BlockPos targetPos = current.relative(chosenDir);
                return new Target(targetPos, chosenDir, false, false);
            }
        }

        // 2) Container vor dem Item → normale Fahrt hinein
        for (Direction d : Direction.values()) {
            BlockPos candidate = current.relative(d);
            if (candidate.equals(lastPos)) continue;
            if (!level.hasChunkAt(candidate)) continue;

            BlockEntity be = level.getBlockEntity(candidate);
            if (be instanceof Container) {
                return new Target(candidate, d, true, false);
            }
        }

        // 3) Prüfen: angrenzende Extractor-Pipes **außerhalb Container**
        for (Direction d : Direction.values()) {
            BlockPos candidate = current.relative(d);
            if (candidate.equals(lastPos)) continue;
            if (!level.hasChunkAt(candidate)) continue;

            BlockState state = level.getBlockState(candidate);
            Block block = state.getBlock();

            if (block instanceof BlockPipeExtract) {
                return new Target(candidate, d, false, false); // Extraktor hat Vorrang
            }
        }

        // 4) Normale Pipes prüfen
        for (Direction d : Direction.values()) {
            BlockPos candidate = current.relative(d);
            if (candidate.equals(lastPos)) continue;
            if (!level.hasChunkAt(candidate)) continue;

            BlockState state = level.getBlockState(candidate);
            Block block = state.getBlock();

            if (block instanceof BlockPipe && !(block instanceof BlockPipeExtract)) {
                return new Target(candidate, d, false, false);
            }
        }

        return null;
    }
}