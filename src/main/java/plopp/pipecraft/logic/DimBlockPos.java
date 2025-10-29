package plopp.pipecraft.logic;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class DimBlockPos {
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    public DimBlockPos(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DimBlockPos other)) return false;
        return dimension.equals(other.dimension) && pos.equals(other.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }

    @Override
    public String toString() {
        return "DimBlockPos{" + dimension.location() + ", " + pos + "}";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.location().toString());
        tag.putLong("Pos", pos.asLong());
        return tag;
    }
    
    public static DimBlockPos load(CompoundTag tag) {
        if (!tag.contains("Dimension") || !tag.contains("Pos")) {
            return null;
        }
        
        ResourceLocation dimId = ResourceLocation.parse(tag.getString("Dimension"));
        ResourceKey<Level> dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
        return new DimBlockPos(dimKey, pos);
    }
}