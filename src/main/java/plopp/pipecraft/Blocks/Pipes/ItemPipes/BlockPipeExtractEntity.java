package plopp.pipecraft.Blocks.Pipes.ItemPipes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.gui.pipes.PipeExtractMenu;

public class BlockPipeExtractEntity extends BlockEntity implements MenuProvider{

	 public BlockPipeExtractEntity(BlockPos pos, BlockState state) {
	        super(BlockEntityRegister.PIPE_EXTRACT.get(), pos, state);
	    }


	 @Override
	 public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
	     return new PipeExtractMenu(containerId, playerInventory, this);
	 }
	 
	 @Override
	 public Component getDisplayName() {
	     return Component.translatable("screen.pipecraft.pipe_extractor");
	 }

}
