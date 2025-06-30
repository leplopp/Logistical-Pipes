package com.plopp.pipecraft.Blocks.Pipes.Viaduct;

import com.plopp.pipecraft.PipeCraftIndex;
import com.plopp.pipecraft.Blocks.BlockEntityRegister;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityViaductLinker extends  BlockEntity implements MenuProvider {
	private ItemStack displayedItem = new ItemStack(BlockRegister.VIADUCTLINKER.get());
	private String customName = "Viaduct Link";
	
    public BlockEntityViaductLinker(BlockPos pos, BlockState state) {
        super(BlockEntityRegister.VIADUCT_LINKER.get(), pos, state);
    }
    public String getCustomName() {
        return customName == null || customName.isEmpty() ? "Viaduct Link" : customName;
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayedItem = stack.copy();
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }


    public void setCustomName(String name) {
        this.customName = (name == null || name.isEmpty()) ? "Viaduct Link" : name;
        System.out.println("[BlockEntity] setCustomName: " + this.customName);
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

	public ItemStack getDisplayedItem() {
	    return displayedItem;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	    super.saveAdditional(tag, registries);
	    tag.putString("CustomName", customName);
	    if (!displayedItem.isEmpty()) {
	        tag.put("DisplayedItem", displayedItem.save(registries));
	    }
	}

            
	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	    super.loadAdditional(tag, registries);
	    if (tag.contains("CustomName", Tag.TAG_STRING)) {
	        customName = tag.getString("CustomName");
	    } else {
	        customName = "Viaduct Link"; // Default
	    }
	    if (tag.contains("DisplayedItem", Tag.TAG_COMPOUND)) {
	        displayedItem = ItemStack.CODEC
	            .parse(NbtOps.INSTANCE, tag.getCompound("DisplayedItem"))
	            .result()
	            .orElse(ItemStack.EMPTY);
	    } else {
	        displayedItem = new ItemStack(BlockRegister.VIADUCTLINKER.get());
	    }
	}

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.pipecraft.viaduct_linker");
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
        this.saveAdditional(tag, lookup); // <- wichtig: dein vollständiges NBT
        return tag;
    }


    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        this.loadAdditional(tag, lookup); // <- korrekt mit Provider
    }
    
}

