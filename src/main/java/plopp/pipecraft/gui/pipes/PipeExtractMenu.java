package plopp.pipecraft.gui.pipes;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.BlockPipeExtractEntity;
import plopp.pipecraft.gui.MenuTypeRegister;

public class PipeExtractMenu extends AbstractContainerMenu {
		
	    public final BlockPipeExtractEntity  blockEntity;
	    private String customName;

	    public PipeExtractMenu(int id, Inventory inv, FriendlyByteBuf buf) {
	        this(id, inv, (BlockPipeExtractEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
	    }

	    public PipeExtractMenu(int containerId, Inventory playerInv, BlockPipeExtractEntity tile) {
	        super(MenuTypeRegister.EXTRACT_PIPE.get(), containerId);
	        this.blockEntity = tile;
     
	      
	        int startX = 8;
	        int startY = 84;
	        for (int row = 0; row < 3; ++row) {
	            for (int col = 0; col < 9; ++col) {
	                this.addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
	            }
	        }
	        for (int col = 0; col < 9; ++col) {
	            this.addSlot(new Slot(playerInv, col, startX + col * 18, startY + 58));
	        }
	    }
	    
	    public void setCustomName(String name) {
	        this.customName = (name == null || name.isEmpty()) ? "Viaduct Connector" : name;
	        
	    }
	    
	    public String getCustomName() {
	        return customName;
	    }
	    
		@Override
		public ItemStack quickMoveStack(Player player, int index) {
			return getCarried();
		}

		@Override
		public boolean stillValid(Player player) {
		    return blockEntity != null && 
		           blockEntity.getLevel() != null && 
		           player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, 
		                                blockEntity.getBlockPos().getY() + 0.5, 
		                                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
		}
	    
	    private class PhantomSlot extends Slot {
	        public PhantomSlot(int x, int y) {
	            super(new Container() {
	                @Override public int getContainerSize() { return 0; }
	                @Override public boolean isEmpty() { return true; }
	                @Override public ItemStack getItem(int index) { return ItemStack.EMPTY; }
	                @Override public ItemStack removeItem(int index, int count) { return ItemStack.EMPTY; }
	                @Override public ItemStack removeItemNoUpdate(int index) { return ItemStack.EMPTY; }
	                @Override public void setItem(int index, ItemStack stack) {}
	                @Override public void setChanged() {}
	                @Override public boolean stillValid(Player player) { return true; }
	                @Override public void clearContent() {}
	            }, 0, x, y);
	        }

	        @Override
	        public boolean mayPlace(ItemStack stack) {
	            return false;
	        }

	        @Override
	        public boolean mayPickup(Player player) {
	            return false;
	        }

	        @Override
	        public ItemStack getItem() {
	        	return ItemStack.EMPTY;
	        }

	        @Override
	        public void set(ItemStack stack) {
	        }
	    }
	    
	    public void saveChanges() {
	        if (!blockEntity.getLevel().isClientSide) {
	            System.out.println("[Menu] saveChanges: saving name '" + customName + "'");
	            blockEntity.setChanged();
	            blockEntity.getLevel().sendBlockUpdated(
	                blockEntity.getBlockPos(),
	                blockEntity.getBlockState(),
	                blockEntity.getBlockState(),
	                3
	            );
	        }
	    }
}
