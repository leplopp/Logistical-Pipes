package plopp.pipecraft.Blocks.Pipes.Viaduct;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.BlockEntityRegister;

public class BlockEntityViaduct extends BlockEntity{

	 public BlockEntityViaduct(BlockPos pos, BlockState state) {
	        super(BlockEntityRegister.VIADUCT.get(), pos, state);
	    }

}
