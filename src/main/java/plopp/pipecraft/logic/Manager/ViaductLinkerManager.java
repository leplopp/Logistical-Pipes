package plopp.pipecraft.logic.Manager;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.Network.data.ViaductLinkerWorldData;
import plopp.pipecraft.Network.linker.LinkedTargetEntry;
import plopp.pipecraft.Network.linker.ViaductLinkerListPacket;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;

public class ViaductLinkerManager {
    private static final Map<BlockPos, DataEntryRecord> allLinkersData = new HashMap<>();
    private static final Map<UUID, ViaductLinkerMenu> openMenus = new HashMap<>();

    public static void addOrUpdateLinker(BlockPos pos, String name, ItemStack icon) {
        pos = pos.immutable();
        DataEntryRecord newRecord = new DataEntryRecord(pos, name, icon);
        DataEntryRecord existing = allLinkersData.get(pos);

        if (!newRecord.equals(existing)) {
            allLinkersData.put(pos, newRecord);
        }
    }

    public static void removeLinker(BlockPos pos) {
        allLinkersData.remove(pos);
    }

    public static Collection<DataEntryRecord> getAllLinkersData() {
        return Collections.unmodifiableCollection(allLinkersData.values());
    }

    public static void setOpenMenu(UUID playerUUID, ViaductLinkerMenu menu) {
        if (menu == null) {
            openMenus.remove(playerUUID);
        } else {
            openMenus.put(playerUUID, menu);
        }
    }

    public static ViaductLinkerMenu getOpenMenu(UUID playerUUID) {
        return openMenus.get(playerUUID);
    }

    public static void loadFromWorldData(ViaductLinkerWorldData data) {
        allLinkersData.clear();
        if (data != null) {
            allLinkersData.putAll(data.getLinkers());
        }
    }

    public static void saveToWorldData(ViaductLinkerWorldData data) {
        if (data != null) {
            data.setLinkers(new HashMap<>(allLinkersData));
        }
    }

    public static void updateOpenLinker(Level level, BlockEntityViaductLinker currentLinker) {
        if (level == null || level.isClientSide || currentLinker == null) return;

        List<LinkedTargetEntry> newTargets = currentLinker.findLinkedTargetsThroughViaducts(true);

        if (!currentLinker.getLinkedTargets().equals(newTargets)) {
            currentLinker.getLinkedTargets().clear();
            currentLinker.getLinkedTargets().addAll(newTargets);
            currentLinker.setChanged();

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(
                    currentLinker.getBlockPos(),
                    level.getBlockState(currentLinker.getBlockPos()),
                    level.getBlockState(currentLinker.getBlockPos()),
                    3
                );
            }
        }

        Set<BlockPos> linkedPositions = currentLinker.getLinkedTargets().stream()
            .map(t -> t.pos)
            .collect(Collectors.toSet());
        linkedPositions.add(currentLinker.getBlockPos()); 

        List<DataEntryRecord> filtered = allLinkersData.values().stream()
            .filter(e -> linkedPositions.contains(e.pos()))
            .sorted(Comparator.comparingDouble(e -> e.pos().distSqr(currentLinker.getBlockPos())))
            .distinct()
            .toList();

        for (ViaductLinkerMenu menu : openMenus.values()) {
            if (menu.blockEntity == currentLinker) {
                menu.updateLinkersData(filtered);
                menu.checkIfAllLoaded(filtered.size());

                if (menu.serverPlayer != null) {
                    NetworkHandler.sendToClient(menu.serverPlayer, new ViaductLinkerListPacket(filtered));
                }
            }
        }
    }
}