package plopp.pipecraft.Network.data;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;

public class ViaductTeleporterWorldData extends SavedData {
    private final Map<BlockPos, TeleporterEntryRecord> data = new HashMap<>();

    public ViaductTeleporterWorldData() {}

    public ViaductTeleporterWorldData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = tag.getList("Teleporters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            TeleporterEntryRecord entry = TeleporterEntryRecord.fromNBT(entryTag, registries);
            data.put(entry.pos(), entry);
        }
    }

    public static ViaductTeleporterWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                ViaductTeleporterWorldData::new,
                ViaductTeleporterWorldData::new,
                null
            ),
            "viaduct_teleporter_data"
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TeleporterEntryRecord entry : data.values()) {
            list.add(entry.toNBT(registries));
        }
        tag.put("Teleporters", list);
        return tag;
    }

    public void setTeleporters(Map<BlockPos, TeleporterEntryRecord> teleporters) {
        data.clear();
        data.putAll(teleporters);
        setDirty();
    }

    public Map<BlockPos, TeleporterEntryRecord> getTeleporters() {
        return data;
    }

    public void setEntry(BlockPos pos, TeleporterEntryRecord entry) {
        data.put(pos, entry);
        setDirty();
    }
    
    public void removeEntry(BlockPos pos) {
        data.remove(pos);
        setDirty();
    }
}