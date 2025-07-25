package plopp.pipecraft.Blocks;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.logic.Connectable;

public class ViaductBlockRegistry {
	
	private static final Set<Block> VIADUCT_BLOCKS = new HashSet<>();

    public static void registerViaductBlock(Block block) {
        VIADUCT_BLOCKS.add(block);
    }

    public static boolean isViaduct(BlockState state) {
        return VIADUCT_BLOCKS.contains(state.getBlock());
    }

    public static boolean isViaduct(Block block) {
        return VIADUCT_BLOCKS.contains(block);
    }

    public static Set<Block> getAllRegistered() {
        return Set.copyOf(VIADUCT_BLOCKS);
    }
    
    public static boolean areViaductBlocksConnected(Level level, BlockPos from, BlockPos to) {
        BlockState fromState = level.getBlockState(from);
        BlockState toState = level.getBlockState(to);

        if (!isViaduct(fromState) || !isViaduct(toState)) return false;

        Block fromBlock = fromState.getBlock();
        Block toBlock = toState.getBlock();

        if (!(fromBlock instanceof Connectable connectableFrom)) return false;
        if (!(toBlock instanceof Connectable connectableTo)) return false;

        // Richtung berechnen
        Direction dir = getDirectionBetween(from, to);
        if (dir == null) return false;

        return connectableFrom.canConnectTo(fromState, level, from, dir) &&
               connectableTo.canConnectTo(toState, level, to, dir.getOpposite());
    }

    private static Direction getDirectionBetween(BlockPos from, BlockPos to) {
        for (Direction dir : Direction.values()) {
            if (from.relative(dir).equals(to)) {
                return dir;
            }
        }
        return null;
    }


}
