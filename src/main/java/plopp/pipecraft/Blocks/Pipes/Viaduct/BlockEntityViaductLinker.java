package plopp.pipecraft.Blocks.Pipes.Viaduct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Network.LinkedTargetEntry;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;
import plopp.pipecraft.logic.ViaductLinkerManager;

public class BlockEntityViaductLinker extends  BlockEntity implements MenuProvider, IBlockEntityExtension  {
	
	private ItemStack displayedItem = new ItemStack(BlockRegister.VIADUCTLINKER.get());
	private String customName = "Viaduct Link";
	final List<LinkedTargetEntry> linkedTargets = new ArrayList<>();
	private List<LinkedTargetEntry> cachedLinkedTargets = new ArrayList<>();
	private List<BlockPos> sortedTargetPositions = new ArrayList<>();
	private CompoundTag customPersistentData = new CompoundTag();
	
    public BlockEntityViaductLinker(BlockPos pos, BlockState state) {
        super(BlockEntityRegister.VIADUCT_LINKER.get(), pos, state);
    }

    public List<LinkedTargetEntry> getLinkedTargets() {
        return linkedTargets;
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            ViaductLinkerManager.addOrUpdateLinker(worldPosition, getCustomName(), getDisplayedItem());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

    }
    
    @Override
    public CompoundTag getPersistentData() {
        return customPersistentData;
    }
    
    public List<BlockPos> getSortedTargetPositions() {
        return Collections.unmodifiableList(sortedTargetPositions);
    }

    public void setSortedTargetPositions(List<BlockPos> positions) {
        this.sortedTargetPositions = new ArrayList<>(positions);
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public void addLinkedTarget(LinkedTargetEntry entry) {
        if (linkedTargets.stream().noneMatch(e -> e.pos.equals(entry.pos))) {
            linkedTargets.add(entry);
            setChanged();
        }
    }

    public void removeLinkedTarget(BlockPos pos) {
        linkedTargets.removeIf(e -> e.pos.equals(pos));
        setChanged();
    }
    
    public String getCustomName() {
        return customName == null || customName.isEmpty() ? "Viaduct Link" : customName;
    }
    
    public void setCustomName(String name) {
        this.customName = (name == null || name.isEmpty()) ? "Viaduct Link" : name;
        setChanged();
        if (!level.isClientSide) {
            ViaductLinkerManager.addOrUpdateLinker(worldPosition, this.customName, this.displayedItem);
            ((ServerLevel) level).sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayedItem = stack.copy();
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }
    
	public ItemStack getDisplayedItem() {
	    return displayedItem;
	}
	
	 @Override
	    public Component getDisplayName() {
	        return Component.translatable("screen.pipecraft.viaduct_linker");
	    }
	
	 @Override
	 protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	     super.saveAdditional(tag, registries);

	     tag.putString("CustomName", customName);
	     if (!displayedItem.isEmpty()) {
	         tag.put("DisplayedItem", displayedItem.save(registries));
	     }

	     ListTag listTag = new ListTag();
	     for (LinkedTargetEntry entry : linkedTargets) {
	         listTag.add(entry.toNBT());
	     }
	     
	     tag.put("LinkedTargets", listTag);
	     ListTag sortedListTag = new ListTag();
	     for (BlockPos pos : sortedTargetPositions) {
	         sortedListTag.add(NbtUtils.writeBlockPos(pos));
	     }
	     
	     tag.put("CustomSortedTargets", sortedListTag);
	 }

	 @Override
	 protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	     super.loadAdditional(tag, registries);

	     if (tag.contains("CustomName", Tag.TAG_STRING)) {
	         customName = tag.getString("CustomName");
	     } else {
	         customName = "Viaduct Link";
	     }

	     if (tag.contains("DisplayedItem", Tag.TAG_COMPOUND)) {
	         displayedItem = ItemStack.CODEC
	             .parse(NbtOps.INSTANCE, tag.getCompound("DisplayedItem"))
	             .result()
	             .orElse(ItemStack.EMPTY);
	     } else {
	         displayedItem = new ItemStack(BlockRegister.VIADUCTLINKER.get());
	     }

	     linkedTargets.clear();
	     if (tag.contains("LinkedTargets", Tag.TAG_LIST)) {
	         ListTag listTag = tag.getList("LinkedTargets", Tag.TAG_COMPOUND);
	         for (int i = 0; i < listTag.size(); i++) {
	             CompoundTag posTag = listTag.getCompound(i);
	             linkedTargets.add(LinkedTargetEntry.fromNBT(posTag));
	         }
	     }

	     if (sortedTargetPositions == null) {
	         sortedTargetPositions = new ArrayList<>();
	     }
	     sortedTargetPositions.clear();

	     if (tag.contains("CustomSortedTargets", Tag.TAG_LIST)) {
	    	    ListTag listTag = tag.getList("CustomSortedTargets", Tag.TAG_INT_ARRAY);
	    	    for (int i = 0; i < listTag.size(); i++) {
	    	        IntArrayTag intArrayTag = (IntArrayTag) listTag.get(i);
	    	        int[] coords = intArrayTag.getAsIntArray();
	    	        if (coords.length == 3) {
	    	            BlockPos pos = new BlockPos(coords[0], coords[1], coords[2]);
	    	            sortedTargetPositions.add(pos);
	    	        }
	    	    }
	    	}
	 }
	 
	 @Override
	 public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
	     return saveWithFullMetadata(lookup);
	 }

	    @Override
	    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
	    	 this.loadAdditional(tag, null);
	    }
	 
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ViaductLinkerMenu(id, inv, this);
    }
    
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    public List<LinkedTargetEntry> findLinkedTargetsThroughViaducts(boolean forceUpdate) {
        if (level == null || level.isClientSide) return Collections.emptyList();
        if (!forceUpdate && !cachedLinkedTargets.isEmpty()) {
            return cachedLinkedTargets;
        }

        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> toBeVisited = new HashSet<>();
        List<LinkedTargetEntry> foundLinkers = new ArrayList<>();
        Queue<BlockPos> toVisit = new ArrayDeque<>();

        visited.add(worldPosition);

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.getBlock() == BlockRegister.VIADUCT.get()) {
                toVisit.add(neighbor);
                toBeVisited.add(neighbor);
            }
        }

        while (!toVisit.isEmpty()) {
            BlockPos current = toVisit.poll();
            toBeVisited.remove(current); 

            if (!visited.add(current)) continue;

            BlockState currentState = level.getBlockState(current);

            for (Direction dir : Direction.values()) {
            	
                if (!BlockViaduct.isConnectedTo(currentState, dir)) continue;

                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor) || toBeVisited.contains(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);
                BlockEntity be = level.getBlockEntity(neighbor);

                if (be instanceof BlockEntityViaductLinker linker && !neighbor.equals(worldPosition)) {
                    String name = linker.getCustomName();
                    foundLinkers.add(new LinkedTargetEntry(neighbor, name));
                } else if (neighborState.getBlock() == BlockRegister.VIADUCT.get()) {
                    toVisit.add(neighbor);
                    toBeVisited.add(neighbor);
                }
            }
        }

        cachedLinkedTargets = foundLinkers;
        return foundLinkers;
    }
}