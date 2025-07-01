package plopp.pipecraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;

public class ConnectionHelper {
	
	public static boolean canConnect(BlockState state, BlockState neighborState) {
	    if (state == null || neighborState == null) return false;
	    Block block = state.getBlock();
	    Block neighborBlock = neighborState.getBlock();

	    if (block instanceof BlockViaduct && neighborBlock instanceof BlockViaduct) return true;
	    if (block instanceof BlockViaductLinker && neighborBlock instanceof BlockViaductLinker) return true;

	    if ((block instanceof BlockViaduct && neighborBlock instanceof BlockViaductLinker) ||
	        (block instanceof BlockViaductLinker && neighborBlock instanceof BlockViaduct)) return true;

	    return false;
	}
	  
	   public static boolean canConnect(Level world, BlockPos pos, Direction direction) {
	        BlockState state = world.getBlockState(pos);
	        BlockPos neighborPos = pos.relative(direction);
	        BlockState neighborState = world.getBlockState(neighborPos);

	        return canConnect(state, neighborState);
	    }
}
