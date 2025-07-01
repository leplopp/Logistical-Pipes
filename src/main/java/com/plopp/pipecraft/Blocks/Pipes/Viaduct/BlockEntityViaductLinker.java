package com.plopp.pipecraft.Blocks.Pipes.Viaduct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import com.plopp.pipecraft.Blocks.BlockEntityRegister;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Network.LinkedTargetEntry;
import com.plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;
import com.plopp.pipecraft.logic.ViaductLinkerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityViaductLinker extends  BlockEntity implements MenuProvider {
	
	private ItemStack displayedItem = new ItemStack(BlockRegister.VIADUCTLINKER.get());
	private String customName = "Viaduct Link";
	final List<LinkedTargetEntry> linkedTargets = new ArrayList<>();
	
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
	}

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ViaductLinkerMenu(id, inv, this);
    }
    
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        this.loadAdditional(pkt.getTag(), registries);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, lookup); 
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        this.loadAdditional(tag, lookup); 
    }

    public List<LinkedTargetEntry> findLinkedTargetsThroughViaducts() {
        if (level == null || level.isClientSide) return Collections.emptyList();

        Set<BlockPos> visited = new HashSet<>();
        List<LinkedTargetEntry> foundLinkers = new ArrayList<>();

        Queue<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(worldPosition);

        while (!toVisit.isEmpty()) {
            BlockPos current = toVisit.poll();
            if (!visited.add(current)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;

                BlockState state = level.getBlockState(neighbor);
                BlockEntity be = level.getBlockEntity(neighbor);

                if (be instanceof BlockEntityViaductLinker linker && !neighbor.equals(worldPosition)) {
                    String name = linker.getCustomName(); // oder Default
                    foundLinkers.add(new LinkedTargetEntry(neighbor, name));
                } else if (state.getBlock() == BlockRegister.VIADUCT.get()) {
                    toVisit.add(neighbor);
                }
            }
        }

        return foundLinkers;
    }
}