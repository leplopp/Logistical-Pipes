package plopp.pipecraft.gui.teleporter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductTeleporter;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.Network.teleporter.PacketUpdateTeleporterToggle;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterManager;
import plopp.pipecraft.gui.MenuTypeRegister;
import plopp.pipecraft.logic.DimBlockPos;

public class ViaductTeleporterMenu extends AbstractContainerMenu {

    public final BlockEntityViaductTeleporter blockEntity;
    private ItemStack startDisplayedItem = ItemStack.EMPTY;
    private ItemStack targetDisplayedItem = ItemStack.EMPTY;
    private String customName = "";
    private String startName = "";
    private String targetName = "";
    private String targetId = "";
    private boolean toggleState = false;
    private final BlockPos blockPos;

    public ViaductTeleporterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (BlockEntityViaductTeleporter) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ViaductTeleporterMenu(int id, Inventory playerInv, BlockEntityViaductTeleporter tile) {
        super(MenuTypeRegister.VIADUCT_TELEPORTER.get(), id);
        this.blockEntity = tile;
        this.customName = tile.getCustomName();
        this.startDisplayedItem = tile.getDisplayedItem() != null ? tile.getDisplayedItem().copy() : ItemStack.EMPTY;
        this.targetDisplayedItem = tile.getTargetDisplayedItem().copy();
        this.blockPos = tile.getBlockPos();
        this.toggleState = blockEntity.isTeleportIdVisible();
      
        if (tile.getStartName() == null || tile.getStartName().isEmpty()) {
            tile.setStartName("Teleport Start");
        }
        if (tile.getTargetName() == null || tile.getTargetName().isEmpty()) {
            tile.setTargetName("Teleport Goal");
        }
        if (tile.getTargetId() == null || tile.getTargetId().isEmpty()) {
            tile.setTargetId("teleporter_goal");
        }

        refreshFromBlockEntity();

        this.addSlot(new PhantomSlot(39, 47, true));
        this.addSlot(new PhantomSlot(121, 47, false));

        // Inventar Slots
        int startX = 8;
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, startX + col * 18, startY + 58));
        }
    }
    
    public void setToggleState(boolean state) {
        this.toggleState = state;

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null &&
            Minecraft.getInstance().level.isClientSide) {
            Minecraft.getInstance().getConnection().send(
                new PacketUpdateTeleporterToggle(blockEntity.getBlockPos(), toggleState)
            );
        }
    }

    public boolean getToggleState() {
        return blockEntity.isTeleportIdVisible();
    }
    
    public void setCustomName(String name) {
        this.customName = name;
    }

    public String getCustomName() {
        return this.customName != null ? this.customName : "";
    }

    public void refreshFromBlockEntity() {
        this.startDisplayedItem = blockEntity.getDisplayedItem() != null ? blockEntity.getDisplayedItem().copy() : ItemStack.EMPTY;
        this.targetDisplayedItem = blockEntity.getTargetDisplayedItem() != null ? blockEntity.getTargetDisplayedItem().copy() : ItemStack.EMPTY;

        this.startName = blockEntity.getStartName();
        if (this.startName == null || this.startName.isEmpty()) {
            this.startName = "Teleport Start";
        }

        this.targetName = blockEntity.getTargetName();
        if (this.targetName == null || this.targetName.isEmpty()) {
            this.targetName = "Teleport Goal";
        }

        this.targetId = blockEntity.getTargetId();
        if (this.targetId == null || this.targetId.isEmpty()) {
            this.targetId = "teleporter_goal";
        }
    }

    public void setStartName(String name) {
        this.startName = name;
    }

    public String getStartName() {
        return (startName == null || startName.isEmpty()) ? "" : startName;
    }

    public void setTargetName(String name) {
        this.targetName = name;
    }

    public String getTargetName() {
        return (targetName == null || targetName.isEmpty()) ? "" : targetName;
    }

    public void setTargetId(String id) {
        this.targetId = id;
    }

    public String getTargetId() {
        return (targetId == null || targetId.isEmpty()) ? "" : targetId;
    }

    public ItemStack getStartDisplayedItem() {
        return startDisplayedItem != null ? startDisplayedItem : ItemStack.EMPTY;
    }

    public ItemStack getTargetDisplayedItem() {
        return targetDisplayedItem != null ? targetDisplayedItem : ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            saveChanges(); 
        }

        if (player instanceof ServerPlayer) {
            DataEntryRecord start = new DataEntryRecord(
                blockPos,
                this.startName,
                this.startDisplayedItem
            );
            DataEntryRecord goal = new DataEntryRecord(
                blockPos,
                this.targetName,
                this.targetDisplayedItem
            );

            ViaductTeleporterManager.updateEntry(blockPos, start, goal, player.getUUID());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId == 0 || slotId == 1) {
            ItemStack held = player.containerMenu.getCarried();

            if (held.isEmpty()) {
            	
                if (slotId == 0) {
                    startDisplayedItem = new ItemStack(Items.BARRIER);
                } else {
                    targetDisplayedItem = new ItemStack(Items.ENDER_PEARL);
                }
            } else {
                if (slotId == 0) {
                    startDisplayedItem = held.copyWithCount(1);
                } else {
                    targetDisplayedItem = held.copyWithCount(1);
                }
            }

            this.slots.get(slotId).setChanged();

            if (!player.level().isClientSide) {
                blockEntity.setDisplayedItem(startDisplayedItem.copy());
                blockEntity.setTargetDisplayedItem(targetDisplayedItem.copy());
                blockEntity.setStartName(startName);
                blockEntity.setTargetName(targetName);
                blockEntity.setTargetId(targetId);
                blockEntity.setCustomName(customName);
                blockEntity.setChanged();
            }
            return;
        }

        super.clicked(slotId, dragType, clickType, player);
    }

    private class PhantomSlot extends Slot {
        private final boolean isStartSlot;

        public PhantomSlot(int x, int y, boolean isStartSlot) {
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
            this.isStartSlot = isStartSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public boolean mayPickup(Player player) { return false; }

        @Override
        public ItemStack getItem() {
            return isStartSlot ? getStartDisplayedItem() : getTargetDisplayedItem();
        }

        @Override
        public void set(ItemStack stack) {}
    }

    public void saveChanges() {
        if (!blockEntity.getLevel().isClientSide) {
            blockEntity.setCustomName(this.customName);
            blockEntity.setStartName(this.startName);
            blockEntity.setTargetName(this.targetName);
            blockEntity.setTargetId(this.targetId);
            blockEntity.setDisplayedItem(this.startDisplayedItem.copy());
            blockEntity.setTargetDisplayedItem(this.targetDisplayedItem.copy());
            blockEntity.setChanged();

            blockEntity.getLevel().sendBlockUpdated(
                blockEntity.getBlockPos(),
                blockEntity.getBlockState(),
                blockEntity.getBlockState(),
                3);
        }
    }
}