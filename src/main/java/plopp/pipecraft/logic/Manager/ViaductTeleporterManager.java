package plopp.pipecraft.logic.Manager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import plopp.pipecraft.Network.data.ViaductTeleporterWorldData;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;
import plopp.pipecraft.logic.DimBlockPos;

public class ViaductTeleporterManager {
    private static final Map<DimBlockPos, TeleporterEntryRecord> teleporters = new HashMap<>();

    public static void loadFromWorldData(ViaductTeleporterWorldData data) {
        teleporters.clear();
        teleporters.putAll(data.getTeleporters());
    }

    public static void saveToWorldData(ViaductTeleporterWorldData data) {
        data.setTeleporters(teleporters);
    }

    public static void setTeleport(ServerLevel level, DimBlockPos pos, TeleporterEntryRecord entry) {
        teleporters.put(pos, entry);
        ViaductTeleporterWorldData.get(level).setEntry(pos, entry);
    }

    public static void removeTeleport(ServerLevel level, DimBlockPos pos) {
        teleporters.remove(pos);
        ViaductTeleporterWorldData.get(level).removeEntry(pos);
    }

    public static TeleporterEntryRecord getEntry(DimBlockPos pos) {
        return teleporters.get(pos);
    }

    public static Map<DimBlockPos, TeleporterEntryRecord> getAll() {
        return teleporters;
    }
    
    private static final TicketType<BlockPos> TEMP_TICKET =
    	    TicketType.create("viaduct_temp", Comparator.comparingLong(BlockPos::asLong));

    	public static void keepTeleporterChunkLoaded(ServerLevel level, BlockPos pos, int ticks) {
    	    ChunkPos chunkPos = new ChunkPos(pos);
    	    level.getChunkSource().addRegionTicket(TEMP_TICKET, chunkPos, 2, pos);

    	    // Ticket nach N Ticks wieder entfernen
    	    level.getServer().execute(() -> {
    	        level.getServer().tell(new TickTask(level.getServer().getTickCount() + ticks, () -> {
    	            level.getChunkSource().removeRegionTicket(TEMP_TICKET, chunkPos, 2, pos);
    	        }));
    	    });
    	}
}