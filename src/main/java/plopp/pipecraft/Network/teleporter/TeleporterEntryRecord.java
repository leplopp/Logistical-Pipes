package plopp.pipecraft.Network.teleporter;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import plopp.pipecraft.Network.data.DataEntryRecordTeleport;
import plopp.pipecraft.logic.DimBlockPos;

public record TeleporterEntryRecord(
	    DimBlockPos dimPos,
	    DataEntryRecordTeleport start,
	    DataEntryRecordTeleport goal,
	    UUID ownerUUID
	) {

	    public static TeleporterEntryRecord fromNBT(CompoundTag tag, HolderLookup.Provider registries) {
	        String dimStr = tag.getString("Dimension");
	        String[] parts = dimStr.split(":", 2);
	        ResourceLocation dimRL = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
	        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimRL);
	        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
	        DimBlockPos dimPos = new DimBlockPos(dim, pos);

	        DataEntryRecordTeleport start = DataEntryRecordTeleport.fromNBT(tag.getCompound("Start"), registries);
	        DataEntryRecordTeleport goal = DataEntryRecordTeleport.fromNBT(tag.getCompound("Goal"), registries);

	        UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;

	        return new TeleporterEntryRecord(dimPos, start, goal, owner);
	    }

	    public CompoundTag toNBT(HolderLookup.Provider registries) {
	        CompoundTag tag = new CompoundTag();
	        tag.putString("Dimension", dimPos.getDimension().location().toString());
	        tag.putLong("Pos", dimPos.getPos().asLong());
	        tag.put("Start", start.toNBT(registries));
	        tag.put("Goal", goal.toNBT(registries));
	        if (ownerUUID != null) {
	            tag.putUUID("Owner", ownerUUID);
	        }
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