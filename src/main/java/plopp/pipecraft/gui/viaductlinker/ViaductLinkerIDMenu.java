package plopp.pipecraft.gui.viaductlinker;

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
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.gui.MenuTypeRegister;

public class ViaductLinkerIDMenu extends AbstractContainerMenu {
	
    public final BlockEntityViaductLinker  blockEntity;
    private ItemStack displayedItem = new ItemStack(BlockRegister.VIADUCTLINKER.get());
    private String customName;

    public ViaductLinkerIDMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (BlockEntityViaductLinker) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ViaductLinkerIDMenu(int containerId, Inventory playerInv, BlockEntityViaductLinker tile) {
        super(MenuTypeRegister.VIADUCT_LINKER_ID.get(), containerId);
        this.blockEntity = tile;
        this.displayedItem = tile.getDisplayedItem().copy();
        this.customName    = tile.getCustomName();
       
        this.displayedItem = blockEntity.getDisplayedItem().copy();
        
        this.addSlot(new PhantomSlot(80, 47));
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
    
    public String getCustomName() {
        return customName;
    }
    
    public void setCustomName(String name) {
        this.customName = (name == null || name.isEmpty()) ? "Viaduct Connector" : name;
        
    }
    
    public String getInitialName() {
        return customName;
    }
    
    public ItemStack getDisplayedItem() {
        return this.displayedItem != null ? this.displayedItem : ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true; 
    }
   
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; 
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    public void setName(String name) {
        this.customName = (name == null || name.isEmpty()) ? "Viaduct Connector" : name;
    }
  
    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId == 0) {
            if (player.level().isClientSide) {
                ItemStack held = Minecraft.getInstance().player.containerMenu.getCarried();
                displayedItem = held.isEmpty()
                    ? new ItemStack(BlockRegister.VIADUCTLINKER.get())
                    : held.copyWithCount(1);
                this.slots.get(0).setChanged();
            } else {
            	
                ItemStack held = player.containerMenu.getCarried();
                displayedItem = held.isEmpty()
                    ? new ItemStack(BlockRegister.VIADUCTLINKER.get())
                    : held.copyWithCount(1);
                this.slots.get(0).setChanged();

                blockEntity.setCustomName(customName);
                blockEntity.setDisplayedItem(displayedItem.copy());
                blockEntity.setChanged();
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
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
            return getDisplayedItem(); 
        }

        @Override
        public void set(ItemStack stack) {
        }
    }
    
    public void saveChanges() {
        if (!blockEntity.getLevel().isClientSide) {
            System.out.println("[Menu] saveChanges: saving name '" + customName + "'");
            blockEntity.setCustomName(customName);
            blockEntity.setDisplayedItem(displayedItem.copy());
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