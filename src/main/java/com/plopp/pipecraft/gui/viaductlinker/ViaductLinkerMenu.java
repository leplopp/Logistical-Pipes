package com.plopp.pipecraft.gui.viaductlinker;

import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import com.plopp.pipecraft.gui.MenuTypeRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ViaductLinkerMenu extends AbstractContainerMenu {

    public final BlockEntityViaductLinker blockEntity;

    public ViaductLinkerMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
    	  this(containerId, inv, (BlockEntityViaductLinker) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ViaductLinkerMenu(int containerId, Inventory inv, BlockEntityViaductLinker tile) {
        super(MenuTypeRegister.VIADUCT_LINKER.get(), containerId);
        this.blockEntity = tile;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}