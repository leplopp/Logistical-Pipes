package plopp.pipecraft.gui.viaductlinker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.Network.LinkedTargetEntryRecord;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.ViaductLinkerListPacket;
import plopp.pipecraft.gui.MenuTypeRegister;
import plopp.pipecraft.logic.ViaductLinkerManager;

public class ViaductLinkerMenu extends AbstractContainerMenu {

    public final BlockEntityViaductLinker blockEntity;
    public final List<Component> linkedNames = new ArrayList<>();
    public final List<ItemStack> linkedItems = new ArrayList<>();
    private List<LinkedTargetEntryRecord> linkers = new ArrayList<>();
	private Level level;
	private List<LinkedTargetEntryRecord> latestLinkers = new ArrayList<>();
	private boolean newDataAvailable = false;
    private boolean allLinkersLoaded = false;
	
    public ViaductLinkerMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
    	  this(containerId, inv, (BlockEntityViaductLinker) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ViaductLinkerMenu(int containerId, Inventory inv, BlockEntityViaductLinker tile) {
        super(MenuTypeRegister.VIADUCT_LINKER.get(), containerId);
        this.blockEntity = tile;
        this.level = inv.player.level(); 
        ViaductLinkerManager.setOpenMenu(this);
        if (!inv.player.level().isClientSide()) {
            Set<BlockPos> connected = tile.getLinkedTargets().stream()
                    .map(e -> e.pos)
                    .collect(Collectors.toSet());

            List<LinkedTargetEntryRecord> linkers = ViaductLinkerManager.getAllLinkersData().stream()
                    .filter(e -> connected.contains(e.pos()))
                    .sorted(Comparator.comparingDouble(e -> e.pos().distSqr(tile.getBlockPos())))
                    .toList();
            
            this.linkers = linkers;
            updateFromLinkers();
            
            ViaductLinkerListPacket packet = new ViaductLinkerListPacket(linkers);
            NetworkHandler.sendToClient((ServerPlayer) inv.player, packet);
        }
    }
    
    public void checkIfAllLoaded(int expectedCount) {
        if (linkers.size() >= expectedCount) {
            allLinkersLoaded = true;
        }
    }

    public boolean isAllLinkersLoaded() {
        return allLinkersLoaded;
    }
     
    @Override
    public void removed(Player player) {
        super.removed(player);
        ViaductLinkerManager.setOpenMenu(null);
    }
    
    public boolean hasNewData() {
        return newDataAvailable;
    }

    public List<LinkedTargetEntryRecord> getLatestLinkers() {
        newDataAvailable = false;
        return latestLinkers;
    }

    public void updateLinkersData(List<LinkedTargetEntryRecord> newLinkers) {
        this.latestLinkers = newLinkers;
        this.newDataAvailable = true;
    }
    
    public void broadcastChanges() {
        super.broadcastChanges();

        if (allLinkersLoaded) return; 

            if (!level.isClientSide) {
                level.getServer().execute(() -> {
                    ViaductLinkerManager.updateAllLinkers(level);
                });
        }

        updateFromLinkers();
    }
    
    public void updateFromLinkers() {
        linkedNames.clear();
        linkedItems.clear();
        for (LinkedTargetEntryRecord entry : linkers) {
            linkedNames.add(Component.literal(entry.name()));
            linkedItems.add(entry.icon());
        }
    }
    
    public void updateLinkedData(List<BlockPos> linkedPositions, Level level) {
        linkedNames.clear();
        linkedItems.clear();

        int foundCount = 0;
        for (BlockPos pos : linkedPositions) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlockEntityViaductLinker linker) {
                linkedNames.add(Component.literal(linker.getCustomName()));
                linkedItems.add(linker.getDisplayedItem());
                foundCount++;
            } else {
                linkedNames.add(Component.literal("Unknown"));
                linkedItems.add(ItemStack.EMPTY);
            }
        }
        
        if (!linkedPositions.isEmpty() && foundCount >= linkedPositions.size()) {
            allLinkersLoaded = true;
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public void setLinkers(List<LinkedTargetEntryRecord> linkers) {
        this.linkers = linkers;
        updateFromLinkers();
    }

    public List<LinkedTargetEntryRecord> getLinkers() {
        return linkers;
    }
}