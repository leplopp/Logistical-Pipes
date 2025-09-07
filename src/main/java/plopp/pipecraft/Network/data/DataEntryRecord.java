package plopp.pipecraft.Network.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

public record DataEntryRecord(BlockPos pos, String name, ItemStack icon) {
    public static DataEntryRecord fromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        int x = tag.getInt("x");
        int y = tag.getInt("y");
        int z = tag.getInt("z");
        String name = tag.getString("Name");

        ItemStack icon = ItemStack.CODEC
            .parse(NbtOps.INSTANCE, tag.getCompound("Icon"))
            .result()
            .orElse(ItemStack.EMPTY);

        return new DataEntryRecord(new BlockPos(x, y, z), name, icon);
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("Name", name);
        tag.put("Icon", icon.save(registries));
        return tag;
    }
}

