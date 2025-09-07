package plopp.pipecraft.Network.teleporter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.Network.data.ViaductTeleporterWorldData;

public class ViaductTeleporterManager {
    private static final Map<BlockPos, TeleporterEntryRecord> teleporters = new HashMap<>();

    public static void loadFromWorldData(ViaductTeleporterWorldData data) {
        teleporters.clear();
        teleporters.putAll(data.getTeleporters());
    }

    public static void saveToWorldData(ViaductTeleporterWorldData data) {
        data.setTeleporters(teleporters);
    }

    public static void updateEntry(BlockPos pos, DataEntryRecord start, DataEntryRecord goal, @Nullable UUID ownerUUID) {
        teleporters.put(pos, new TeleporterEntryRecord(pos, start, goal, ownerUUID));
    }
    
    public static TeleporterEntryRecord getEntry(BlockPos pos) {
        return teleporters.get(pos);
    }

    public static Map<BlockPos, TeleporterEntryRecord> getAll() {
        return teleporters;
    }
    
    public static void removeEntry(ServerLevel level, BlockPos pos) {
        teleporters.remove(pos);
        ViaductTeleporterWorldData.get(level).removeEntry(pos); 
    }
}