package plopp.pipecraft.Blocks;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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
}
