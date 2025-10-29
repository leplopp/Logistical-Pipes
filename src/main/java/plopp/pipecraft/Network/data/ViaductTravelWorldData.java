package plopp.pipecraft.Network.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class ViaductTravelWorldData extends SavedData {
	private final Map<UUID, CompoundTag> activeTravels = new HashMap<>();

	public ViaductTravelWorldData() {
	}

	public ViaductTravelWorldData(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = tag.getList("ActiveTravels", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entryTag = list.getCompound(i);
			if (entryTag.hasUUID("Player")) {
				UUID uuid = entryTag.getUUID("Player");
				CompoundTag dataTag = entryTag.getCompound("Data");
				activeTravels.put(uuid, dataTag);
			}
		}
	}

	public static ViaductTravelWorldData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(
				new SavedData.Factory<>(ViaductTravelWorldData::new, ViaductTravelWorldData::new, null),
				"viaduct_travel_data");
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, CompoundTag> entry : activeTravels.entrySet()) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putUUID("Player", entry.getKey());
			entryTag.put("Data", entry.getValue());
			list.add(entryTag);
		}
		tag.put("ActiveTravels", list);
		return tag;
	}

	public void setTravel(UUID playerId, CompoundTag travelData) {
		activeTravels.put(playerId, travelData);
		setDirty();
	}

	public CompoundTag getTravel(UUID playerId) {
		return activeTravels.get(playerId);
	}

	public boolean hasTravel(UUID playerId) {
		return activeTravels.containsKey(playerId);
	}

	public void removeTravel(UUID playerId) {
		activeTravels.remove(playerId);
		setDirty();
	}

	public Map<UUID, CompoundTag> getAllTravels() {
		return activeTravels;
	}
}