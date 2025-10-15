package plopp.pipecraft.logic;

import java.util.ArrayList;
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
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.Network.data.ViaductLinkerWorldData;
import plopp.pipecraft.Network.linker.LinkedTargetEntry;
import plopp.pipecraft.Network.linker.ViaductLinkerListPacket;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;

public class ViaductLinkerManager {
	
	    private static final Map<BlockPos, DataEntryRecord> allLinkersData = new HashMap<>();
	    private static final Map<BlockPos, DataEntryRecord> knownLinkers = new HashMap<>();
	    private static ViaductLinkerMenu openMenu = null;
	    
	    public static void addOrUpdateLinker(BlockPos pos, String name, ItemStack icon) {
	        pos = pos.immutable();
	        DataEntryRecord newRecord = new DataEntryRecord(pos, name, icon);
	        DataEntryRecord existing = allLinkersData.get(pos);

	        if (!newRecord.equals(existing)) {
	            allLinkersData.put(pos, newRecord);
	            knownLinkers.put(pos, newRecord);
	        }
	    }

	    public static void removeLinker(BlockPos pos) {
	        knownLinkers.remove(pos);
	        allLinkersData.remove(pos);
	    }

	    public static Set<BlockPos> getAllLinkers() {
	        return Collections.unmodifiableSet(allLinkersData.keySet());
	    }

	    public static Collection<DataEntryRecord> getAllLinkersData() {
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
	    
	    public static void updateOpenLinker(Level level) {
	        if (level == null || level.isClientSide) return;
	        if (openMenu == null) return;

	        BlockEntityViaductLinker currentLinker = openMenu.blockEntity;
	        if (currentLinker == null) return;

	        List<LinkedTargetEntry> newTargets = currentLinker.findLinkedTargetsThroughViaducts(true);
	        if (!currentLinker.getLinkedTargets().equals(newTargets)) {
	            currentLinker.getLinkedTargets().clear();
	            currentLinker.getLinkedTargets().addAll(newTargets);
	            currentLinker.setChanged();

	            if (level instanceof ServerLevel serverLevel) {
	                serverLevel.sendBlockUpdated(currentLinker.getBlockPos(), level.getBlockState(currentLinker.getBlockPos()), level.getBlockState(currentLinker.getBlockPos()), 3);
	            }

	            Set<BlockPos> connectedPositions = currentLinker.getLinkedTargets().stream()
	                .map(t -> t.pos)
	                .collect(Collectors.toSet());

	            BlockPos currentPos = currentLinker.getBlockPos();

	            List<DataEntryRecord> filtered = allLinkersData.values().stream()
	                .filter(e -> connectedPositions.contains(e.pos()) || e.pos().equals(currentPos))
	                .sorted(Comparator.comparingDouble(e -> e.pos().distSqr(currentPos)))
	                .toList();

	            if (filtered.stream().noneMatch(e -> e.pos().equals(currentPos))) {
	                filtered = new ArrayList<>(filtered);
	                filtered.add(new DataEntryRecord(
	                    currentPos,
	                    currentLinker.getCustomName(),
	                    currentLinker.getDisplayedItem()
	                ));
	            }
	            
	            if (openMenu != null) {
	                openMenu.updateLinkersData(filtered);
	                openMenu.checkIfAllLoaded(filtered.size());
	                
	                if (openMenu.serverPlayer != null) {
	                    NetworkHandler.sendToClient(openMenu.serverPlayer, new ViaductLinkerListPacket(filtered));
	                }
	           }
	      }
	 }
}