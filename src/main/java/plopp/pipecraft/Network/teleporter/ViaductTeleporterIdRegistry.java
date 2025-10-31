package plopp.pipecraft.Network.teleporter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ViaductTeleporterIdRegistry {
    private static final Map<String, TeleporterEntryRecord> TELEPORTERS_BY_ID = new HashMap<>();
    private static final Map<BlockPos, String> ID_BY_POS = new HashMap<>();

    public static boolean isIdTaken(String id) {
        return TELEPORTERS_BY_ID.containsKey(id);
    }

    public static void registerTeleporter(String id, TeleporterEntryRecord record) {
        TELEPORTERS_BY_ID.put(id, record);
        ID_BY_POS.put(record.pos(), id);
        System.out.println("[TeleporterRegistry] Registriert: " + id + " @ " + record.pos());
    }

    public static void unregisterTeleporter(String id) {
        TeleporterEntryRecord rec = TELEPORTERS_BY_ID.remove(id);
        if (rec != null) {
            ID_BY_POS.remove(rec.pos());
            System.out.println("[TeleporterRegistry] Entfernt: " + id);
        }
    }

    public static void unregisterAt(BlockPos pos) {
        String id = ID_BY_POS.remove(pos);
        if (id != null) TELEPORTERS_BY_ID.remove(id);
    }

    public static TeleporterEntryRecord getRecordById(String id) {
        return TELEPORTERS_BY_ID.get(id);
    }

    public static BlockPos getPositionForId(String id) {
        TeleporterEntryRecord record = TELEPORTERS_BY_ID.get(id);
        return record != null ? record.pos() : null;
    }

    public static void clear() {
        TELEPORTERS_BY_ID.clear();
        ID_BY_POS.clear();
    }

    public static void printAll() {
        System.out.println("[TeleporterRegistry] --- Aktuelle Teleporter ---");
        TELEPORTERS_BY_ID.forEach((id, rec) ->
            System.out.println("  ID: " + id + " @ " + rec.pos() + " | Start=" + rec.start().name() + " | Ziel=" + rec.goal().name())
        );
    }
}