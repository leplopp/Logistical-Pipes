package plopp.pipecraft.Network.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import plopp.pipecraft.logic.pipe.TravellingItem;

public record TravellingItemRecord(
        ItemStack stack,
        BlockPos lastPos,
        BlockPos currentPos,
        Direction side,
        double progress,
        double speed
) {
    public TravellingItemRecord(TravellingItem item) {
        this(
            item.stack.copy(),
            item.lastPos,
            item.currentPos,
            item.side,
            item.progress,
            item.speed
        );
    }

    public static TravellingItemRecord fromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("Stack"));
        BlockPos last = BlockPos.of(tag.getLong("LastPos"));
        BlockPos current = BlockPos.of(tag.getLong("CurrentPos"));
        Direction side = Direction.values()[tag.getInt("Side")];
        double progress = tag.getDouble("Progress");
        double speed = tag.getDouble("Speed");

        return new TravellingItemRecord(stack, last, current, side, progress, speed);
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Stack", stack.save(registries));
        tag.putLong("LastPos", lastPos.asLong());
        tag.putLong("CurrentPos", currentPos.asLong());
        tag.putInt("Side", side.ordinal());
        tag.putDouble("Progress", progress);
        tag.putDouble("Speed", speed);
        return tag;
    }
}