package plopp.pipecraft.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DimBlockPos {
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    public DimBlockPos(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DimBlockPos other)) return false;
        return dimension.equals(other.dimension) && pos.equals(other.pos);
    }

    @Override
    public int hashCode() {
        return 31 * dimension.hashCode() + pos.hashCode();
    }

    @Override
    public String toString() {
        return "DimBlockPos{" + dimension.location() + ", " + pos + "}";
    }
    
    public BlockPos getPos() {
        return pos;
    }
    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}