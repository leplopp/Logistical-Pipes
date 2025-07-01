package com.plopp.pipecraft.Network;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;


public class ViaductLinkerWorldData extends SavedData {
    private Map<BlockPos, LinkedTargetEntryRecord> data = new HashMap<>();

    public ViaductLinkerWorldData() {
        super();
    }

    public ViaductLinkerWorldData(CompoundTag tag, HolderLookup.Provider registries) {
        super();
        data.clear();

        if (tag.contains("ViaductLinkers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("ViaductLinkers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                LinkedTargetEntryRecord entry = LinkedTargetEntryRecord.fromNBT(entryTag, registries);
                data.put(entry.pos(), entry);
            }
        }
    }

    public static ViaductLinkerWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                ViaductLinkerWorldData::new,
                ViaductLinkerWorldData::new,
                null
            ),
            "viaduct_linker_data"
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (LinkedTargetEntryRecord entry : data.values()) {
            list.add(entry.toNBT(registries));
        }
        tag.put("ViaductLinkers", list);
        return tag;
    }

    public void setLinkers(Map<BlockPos, LinkedTargetEntryRecord> linkers) {
        this.data = linkers;
        setDirty();
    }

    public Map<BlockPos, LinkedTargetEntryRecord> getLinkers() {
        return data;
    }
}