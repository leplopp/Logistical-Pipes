package plopp.pipecraft.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface Connectable {
        boolean canConnectTo(BlockState state, Level level, BlockPos pos, Direction direction);
    }


