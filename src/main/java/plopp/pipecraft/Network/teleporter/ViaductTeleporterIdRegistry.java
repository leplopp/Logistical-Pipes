package plopp.pipecraft.Network.teleporter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;

public class ViaductTeleporterIdRegistry {
    private static final Set<String> REGISTERED_IDS = new HashSet<>();
    private static final Map<BlockPos, TeleporterEntryRecord> teleporters = new HashMap<>();
    
    public static boolean isIdTaken(String id) {
        return REGISTERED_IDS.contains(id);
    }

    public static boolean registerId(String id) {
        return REGISTERED_IDS.add(id);
    }

    public static void unregisterId(String id) {
        REGISTERED_IDS.remove(id);
    }

    public static void clear() {
        REGISTERED_IDS.clear();
    }
    
    public static TeleporterEntryRecord getById(String id) {
        for (TeleporterEntryRecord record : teleporters.values()) {
            if (record.goal() != null && id.equals(record.goal().name())) {
                return record;
            }
        }
        return null;
    }
}