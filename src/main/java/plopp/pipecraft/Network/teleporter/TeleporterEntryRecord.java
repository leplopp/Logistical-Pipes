package plopp.pipecraft.Network.teleporter;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import plopp.pipecraft.Network.data.DataEntryRecord;

public record TeleporterEntryRecord(
	    BlockPos pos,
	    DataEntryRecord start,
	    DataEntryRecord goal,
	    UUID ownerUUID 
	) {
	    public static TeleporterEntryRecord fromNBT(CompoundTag tag, HolderLookup.Provider registries) {
	        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
	        CompoundTag startTag = tag.getCompound("Start");
	        CompoundTag goalTag = tag.getCompound("Goal");
	        DataEntryRecord start = DataEntryRecord.fromNBT(startTag, registries);
	        DataEntryRecord goal = DataEntryRecord.fromNBT(goalTag, registries);

	        UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;

	        return new TeleporterEntryRecord(pos, start, goal, owner);
	    }

	    public CompoundTag toNBT(HolderLookup.Provider registries) {
	        CompoundTag tag = new CompoundTag();
	        tag.putInt("x", pos.getX());
	        tag.putInt("y", pos.getY());
	        tag.putInt("z", pos.getZ());
	        tag.put("Start", start.toNBT(registries));
	        tag.put("Goal", goal.toNBT(registries));
	        if (ownerUUID != null) {
	            tag.putUUID("Owner", ownerUUID);
	        }
	        return tag;
	    }
	}