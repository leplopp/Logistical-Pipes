package plopp.pipecraft.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.Network.LinkedTargetEntry;
import plopp.pipecraft.Network.LinkedTargetEntryRecord;
import plopp.pipecraft.Network.ViaductLinkerWorldData;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;

public class ViaductLinkerManager {
	
	    private static final Map<BlockPos, LinkedTargetEntryRecord> allLinkersData = new HashMap<>();
	    private static final Map<BlockPos, LinkedTargetEntryRecord> knownLinkers = new HashMap<>();
	    private static ViaductLinkerMenu openMenu = null;
	    
	    public static void addOrUpdateLinker(BlockPos pos, String name, ItemStack icon) {
	        pos = pos.immutable();
	        LinkedTargetEntryRecord newRecord = new LinkedTargetEntryRecord(pos, name, icon);
	        LinkedTargetEntryRecord existing = allLinkersData.get(pos);

	        if (!newRecord.equals(existing)) {
	            allLinkersData.put(pos, newRecord);
	            knownLinkers.put(pos, newRecord);
	            System.out.println("[ViaductLinkerManager] Updated: " + pos + " | Total=" + allLinkersData.size());
	        }
	    }

	    public static void removeLinker(BlockPos pos) {
	        knownLinkers.remove(pos);
	        allLinkersData.remove(pos);
	        System.out.println("[ViaductLinkerManager] removeLinker: pos=" + pos + " total allLinkers=" + allLinkersData.size());
	    }

	    public static Set<BlockPos> getAllLinkers() {
	        return Collections.unmodifiableSet(allLinkersData.keySet());
	    }

	    public static Collection<LinkedTargetEntryRecord> getAllLinkersData() {
	        return Collections.unmodifiableCollection(allLinkersData.values());
	    }
	    
	    public static void loadFromWorldData(ViaductLinkerWorldData data) {
	        allLinkersData.clear();
	        allLinkersData.putAll(data.getLinkers());
	    }
	    public static void setOpenMenu(ViaductLinkerMenu menu) {
	        openMenu = menu;
	    }

	    public static ViaductLinkerMenu getOpenMenu() {
	        return openMenu;
	    }
	    
	    public static void saveToWorldData(ViaductLinkerWorldData data) {
	        data.setLinkers(new HashMap<>(allLinkersData));
	    }
	    
	    public static void updateAllLinkers(Level level) {
	        if (level == null || level.isClientSide) return;

	        boolean anyChanges = false;

	        for (BlockPos pos : allLinkersData.keySet()) {
	            BlockEntity be = level.getBlockEntity(pos);
	            if (be instanceof BlockEntityViaductLinker linker) {
	                List<LinkedTargetEntry> newTargets = linker.findLinkedTargetsThroughViaducts();
	                if (!linker.getLinkedTargets().equals(newTargets)) {
	                    linker.getLinkedTargets().clear();
	                    linker.getLinkedTargets().addAll(newTargets);
	                    linker.setChanged();

	                    if (level instanceof ServerLevel serverLevel) {
	                        serverLevel.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
	                    }
	                    anyChanges = true;
	                }
	            }
	        }

	        if (anyChanges && openMenu != null) {

	            BlockEntityViaductLinker currentLinker = openMenu.blockEntity;
	            if (currentLinker != null) {
	                Set<BlockPos> connectedPositions = currentLinker.getLinkedTargets().stream()
	                    .map(t -> t.pos)
	                    .collect(Collectors.toSet());

	                List<LinkedTargetEntryRecord> filtered = allLinkersData.values().stream()
	                    .filter(e -> connectedPositions.contains(e.pos()))
	                    .sorted(Comparator.comparingDouble(e -> e.pos().distSqr(currentLinker.getBlockPos())))
	                    .toList();

	                openMenu.updateLinkersData(filtered);
	                openMenu.checkIfAllLoaded(filtered.size());
	            }
	        }
	    }
	}