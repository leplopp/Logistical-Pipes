package plopp.pipecraft.Network.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import plopp.pipecraft.logic.pipe.TravellingItem;


public class PipeTravelWorldData extends SavedData {

    private final List<TravellingItemRecord> items = new ArrayList<>();

    public PipeTravelWorldData() {}

    public PipeTravelWorldData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = tag.getList("TravellingItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            TravellingItemRecord rec = TravellingItemRecord.fromNBT(itemTag, registries);
            items.add(rec);
        }
    }

    public static PipeTravelWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                PipeTravelWorldData::new,
                PipeTravelWorldData::new,
                null
            ),
            "pipe_travel_data"
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TravellingItemRecord rec : items) {
        	   if (!rec.stack().isEmpty() && rec.stack().getCount() > 0) { // <-- nur nicht-leere Items speichern
        	        list.add(rec.toNBT(registries));
        	    }
        }
        tag.put("TravellingItems", list);
        return tag;
    }

    public void setItems(Collection<TravellingItem> activeItems) {
        items.clear();
        for (TravellingItem item : activeItems) {
            items.add(new TravellingItemRecord(item));
        }
        setDirty();
    }

    public List<TravellingItemRecord> getItems() {
        return items;
    }
}