package plopp.pipecraft.logic.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class Target {
    public final BlockPos pos;
    public final Direction side;
    public final boolean isContainer; 
    public final boolean isInContainer;

    public Target(BlockPos pos, Direction side, boolean isContainer,  boolean isInContainer) {
        this.pos = pos;
        this.side = side;
        this.isContainer = isContainer;
        this.isInContainer = isInContainer;
    }
}