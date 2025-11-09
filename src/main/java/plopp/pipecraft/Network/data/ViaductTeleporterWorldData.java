package plopp.pipecraft.Network.data;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;
import plopp.pipecraft.logic.DimBlockPos;

public class ViaductTeleporterWorldData extends SavedData {
	private final Map<DimBlockPos, TeleporterEntryRecord> data = new HashMap<>();

	public ViaductTeleporterWorldData() {
	}

	public ViaductTeleporterWorldData(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = tag.getList("Teleporters", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entryTag = list.getCompound(i);
			TeleporterEntryRecord entry = TeleporterEntryRecord.fromNBT(entryTag, registries);

			DimBlockPos dimPos = DimBlockPos.load(entryTag);
			if (dimPos != null) {
				data.put(dimPos, entry);
			}
		}
	}

	public void setTeleporters(Map<DimBlockPos, TeleporterEntryRecord> teleporters) {
		data.clear();
		data.putAll(teleporters);
		setDirty();
	}

	public static ViaductTeleporterWorldData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(
				new SavedData.Factory<>(ViaductTeleporterWorldData::new, ViaductTeleporterWorldData::new, null),
				"viaduct_teleporter_data");
	}

	public Map<DimBlockPos, TeleporterEntryRecord> getTeleporters() {
		return data;
	}

	public void setEntry(DimBlockPos pos, TeleporterEntryRecord entry) {
		data.put(pos, entry);
		setDirty();
	}

	public void removeEntry(DimBlockPos pos) {
		data.remove(pos);
		setDirty();
	}

	public TeleporterEntryRecord getEntry(DimBlockPos pos) {
		return data.get(pos);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<DimBlockPos, TeleporterEntryRecord> e : data.entrySet()) {
			CompoundTag entryTag = e.getValue().toNBT(registries);
			DimBlockPos dimPos = e.getKey();
			entryTag.putString("Dimension", dimPos.getDimension().location().toString());
			entryTag.putLong("Pos", dimPos.getPos().asLong());
			list.add(entryTag);
		}
		tag.put("Teleporters", list);
		return tag;
	}
}