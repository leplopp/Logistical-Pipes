package plopp.pipecraft.Network.teleporter;

import java.util.HashMap;
import java.util.Map;
import plopp.pipecraft.logic.DimBlockPos;

public class ViaductTeleporterIdRegistry {
    private static final Map<String, TeleporterEntryRecord> TELEPORTERS_BY_ID = new HashMap<>();
    public static final Map<DimBlockPos, String> ID_BY_POS = new HashMap<>();

    public static boolean isIdTaken(String id) {
        return TELEPORTERS_BY_ID.containsKey(id);
    }

    public static void registerTeleporter(String id, TeleporterEntryRecord record) {
        TELEPORTERS_BY_ID.put(id, record);
        ID_BY_POS.put(new DimBlockPos(record.start().dimension(), record.start().pos()), id);
    }

    public static void unregisterTeleporter(String id) {
        TeleporterEntryRecord rec = TELEPORTERS_BY_ID.remove(id);
        if (rec != null) {
            ID_BY_POS.remove(new DimBlockPos(rec.start().dimension(), rec.start().pos()));
        }
    }

    public static void unregisterAt(DimBlockPos dimPos) {
        String id = ID_BY_POS.remove(dimPos);
        if (id != null) TELEPORTERS_BY_ID.remove(id);
    }

    public static TeleporterEntryRecord getRecordById(String id) {
        return TELEPORTERS_BY_ID.get(id);
    }

    public static DimBlockPos getDimPosForId(String id) {
        TeleporterEntryRecord record = TELEPORTERS_BY_ID.get(id);
        return record != null ? new DimBlockPos(record.start().dimension(), record.start().pos()) : null;
    }

    public static void clear() {
        TELEPORTERS_BY_ID.clear();
        ID_BY_POS.clear();
    }
}