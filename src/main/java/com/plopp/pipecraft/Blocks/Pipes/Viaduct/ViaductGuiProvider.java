package com.plopp.pipecraft.Blocks.Pipes.Viaduct;

import com.plopp.pipecraft.gui.viaductlinker.ViaductLinkerIDMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ViaductGuiProvider implements MenuProvider {
	 
	private final BlockPos pos;

	 public ViaductGuiProvider(BlockPos pos) {
		   this.pos = pos;
	    }

	    @Override
	    public Component getDisplayName() {
	    	  return Component.translatable("screen.pipecraft.viaduct_linker_id");
	    }

	    @Override
	    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
	        BlockEntity be = player.level().getBlockEntity(pos);
	        if (!(be instanceof BlockEntityViaductLinker)) {
	            return null;
	        }
	        return new ViaductLinkerIDMenu(id, inv, (BlockEntityViaductLinker) be);
	    }
}
