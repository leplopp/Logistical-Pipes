package plopp.pipecraft.logic.pipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.Blocks.Pipes.BlockPipe;
import plopp.pipecraft.Blocks.Pipes.BlockPipeExtract;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.data.PipeTravelWorldData;
import plopp.pipecraft.Network.pipes.TravellingItemRemovePacket;
import plopp.pipecraft.Network.pipes.TravellingItemSyncPacket;

public class PipeTravel {
	public static final List<TravellingItem> activeItems = new ArrayList<>();

	public static void tick(Level level) {
	    if (activeItems.isEmpty()) return;

	    List<TravellingItem> toRemove = new ArrayList<>();
	    boolean isServer = level instanceof ServerLevel;
	    ServerLevel serverLevel = isServer ? (ServerLevel) level : null;

	    for (TravellingItem item : List.copyOf(activeItems)) {
	        if (isServer) {
	            item.tick(serverLevel);

	            if (item.isFinished()) {
	                toRemove.add(item);

	                TravellingItemRemovePacket removePkt = new TravellingItemRemovePacket(item.id);
	                serverLevel.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(item.currentPos)) < 64*64)
	                           .forEach(p -> NetworkHandler.sendToClient(p, removePkt));

	                continue; 
	            }
	            
	            if (!serverLevel.getServer().isDedicatedServer()) continue;
	            if (!item.stack.isEmpty()) {
	                TravellingItemSyncPacket pkt = new TravellingItemSyncPacket(item);
	                serverLevel.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(item.currentPos)) < 64*64)
	                           .forEach(p -> NetworkHandler.sendToClient(p, pkt));
	            }
	        } else {
	            item.clientTick(level);
	        }
	    }


	    activeItems.removeAll(toRemove);

	    if (isServer) {
	        PipeTravelWorldData.get(serverLevel).setItems(activeItems);
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
  
        if (be instanceof Container container) {
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
            if (!leftover.isEmpty()) {
                spawnItemEntity(level, item.currentPos, leftover);
            }
        } else {
            spawnItemEntity(level, item.currentPos, item.stack);
        }
        item.stack.setCount(0);
    }

    public static void spawnItemEntity(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level,pos.getX() + 0.5,pos.getY() + 0.5,pos.getZ() + 0.5,stack);
        level.addFreshEntity(entity);
    }

    @SuppressWarnings("deprecation")
	public static Target getNextTarget(TravellingItem item, BlockPos current, BlockPos lastPos, Direction side) {
        ServerLevel level = item.level;
        BlockState currentState = level.getBlockState(current);

        boolean isExtractorPipe = currentState.getBlock() instanceof BlockPipeExtract;

        IItemHandler hereInv = level.getCapability(Capabilities.ItemHandler.BLOCK, current, side);
        if (hereInv != null) {
            if (!item.justExtracted) {
                return new Target(current, side, false, true);
            }
            
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
        
        List<Target> possibleTargets = new ArrayList<>();
        for (Direction d : Direction.values()) {
            BlockPos candidate = current.relative(d);
            if (candidate.equals(lastPos)) continue;
            if (!level.hasChunkAt(candidate)) continue;

            BlockState state = level.getBlockState(candidate);
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

        return null; 
    }
    
    public static void cleanupOnWorldUnload(ServerLevel level) {
        PipeTravelWorldData data = PipeTravelWorldData.get(level);
        data.setItems(PipeTravel.activeItems);

        PipeTravel.activeItems.clear();
        TravellingItem.containerExtractorIndex.clear();
        TravellingItem.pipeDirectionIndex.clear();
    }
}