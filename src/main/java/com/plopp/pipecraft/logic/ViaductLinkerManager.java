package com.plopp.pipecraft.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import com.plopp.pipecraft.Network.LinkedTargetEntry;
import com.plopp.pipecraft.Network.LinkedTargetEntryRecord;
import com.plopp.pipecraft.Network.ViaductLinkerWorldData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ViaductLinkerManager {
	
	    private static final Map<BlockPos, LinkedTargetEntryRecord> allLinkersData = new HashMap<>();
	    private static final Map<BlockPos, LinkedTargetEntryRecord> knownLinkers = new HashMap<>();
	    private static List<LinkedTargetEntryRecord> cachedSortedLinkers = new ArrayList<>();
	    private static boolean cacheInvalidated = true;
	    
	    public static void addOrUpdateLinker(BlockPos pos, String name, ItemStack icon) {
	        pos = pos.immutable();
	        LinkedTargetEntryRecord record = new LinkedTargetEntryRecord(pos, name, icon);
	        allLinkersData.put(pos, record);
	        knownLinkers.put(pos, record);
	        cacheInvalidated = true;
	        System.out.println("[ViaductLinkerManager] addOrUpdateLinker: pos=" + pos + " total allLinkers=" + allLinkersData.size());
	    }

	    public static void removeLinker(BlockPos pos) {
	        knownLinkers.remove(pos);
	        allLinkersData.remove(pos);
	        cacheInvalidated = true;
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

	    public static void saveToWorldData(ViaductLinkerWorldData data) {
	        data.setLinkers(new HashMap<>(allLinkersData));
	    }
	    public static void updateAllLinkers(Level level) {
	        if (level == null || level.isClientSide) return;

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
	                }
	            }
	        }
	    
	    }
	}