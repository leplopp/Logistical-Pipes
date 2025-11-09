package plopp.pipecraft.Network.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import plopp.pipecraft.logic.DimBlockPos;

public record DataEntryRecordTeleport(DimBlockPos dimPos, String name, ItemStack icon) {

    public static DataEntryRecordTeleport fromNBT(CompoundTag tag, HolderLookup.Provider registries) {
    	
        String dimStr = tag.getString("Dimension"); 
        String[] parts = dimStr.split(":", 2);
        ResourceLocation dimRL = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimRL);
        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
        String name = tag.getString("Name");
        ItemStack icon = ItemStack.CODEC
            .parse(NbtOps.INSTANCE, tag.getCompound("Icon"))
            .result()
            .orElse(ItemStack.EMPTY);

        return new DataEntryRecordTeleport(new DimBlockPos(dim, pos), name, icon);
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimPos.getDimension().location().toString());
        tag.putLong("Pos", dimPos.getPos().asLong());
        tag.putString("Name", name);
        tag.put("Icon", icon.save(registries));
        return tag;
    }

    public DimBlockPos dimPos() {
        return dimPos;
    }

    public BlockPos pos() {
        return dimPos.getPos();
    }

    public ResourceKey<Level> dimension() {
        return dimPos.getDimension();
    }
}