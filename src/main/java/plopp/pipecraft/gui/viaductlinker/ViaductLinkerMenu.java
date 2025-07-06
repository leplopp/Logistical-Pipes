package plopp.pipecraft.gui.viaductlinker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
    private List<LinkedTargetEntryRecord> customSortedLinkers = new ArrayList<>();
	public ServerPlayer serverPlayer;
	
    
    public ViaductLinkerMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
    	  this(containerId, inv, (BlockEntityViaductLinker) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ViaductLinkerMenu(int containerId, Inventory inv, BlockEntityViaductLinker tile) {
        super(MenuTypeRegister.VIADUCT_LINKER.get(), containerId);
        this.blockEntity = tile;
        this.level = inv.player.level();

        if (inv.player instanceof ServerPlayer sp) {
            this.serverPlayer = sp;
            ViaductLinkerManager.setOpenMenu(this);

            sp.level().sendBlockUpdated(tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), 3);

            // ✨ Das fehlte bisher – überträgt BlockEntity-Daten (inkl. CustomSortedTargets) an Client
            ClientboundBlockEntityDataPacket packet = tile.getUpdatePacket();
            if (packet != null) {
                sp.connection.send(packet);
            }

            sp.server.execute(() -> ViaductLinkerManager.updateOpenLinker(level));
        }

        if (!inv.player.level().isClientSide()) {
            Set<BlockPos> connected = tile.getLinkedTargets().stream()
                    .map(e -> e.pos)
                    .collect(Collectors.toSet());

            this.linkers = ViaductLinkerManager.getAllLinkersData().stream()
                    .filter(e -> connected.contains(e.pos()))
                    .toList();

            List<BlockPos> savedOrder = blockEntity.getSortedTargetPositions();
            if (!savedOrder.isEmpty()) {
                List<LinkedTargetEntryRecord> sortedList = new ArrayList<>();
                List<LinkedTargetEntryRecord> unsorted = new ArrayList<>(this.linkers);

                for (BlockPos pos : savedOrder) {
                    for (Iterator<LinkedTargetEntryRecord> it = unsorted.iterator(); it.hasNext(); ) {
                        LinkedTargetEntryRecord entry = it.next();
                        if (entry.pos().equals(pos)) {
                            sortedList.add(entry);
                            it.remove();
                            break;
                        }
                    }
                }

                // Hänge verbleibende (nicht gespeicherte) hinten dran
                sortedList.addAll(unsorted);

                this.customSortedLinkers = sortedList;
                setLinkers(sortedList);
            } else {
                setLinkers(this.linkers);
            }

            ViaductLinkerListPacket packet = new ViaductLinkerListPacket(
            	    this.customSortedLinkers.isEmpty() ? this.linkers : this.customSortedLinkers
            	);
            	NetworkHandler.sendToClient((ServerPlayer) inv.player, packet);
        }
    }
    public List<LinkedTargetEntryRecord> getCustomSortedLinkers() {
        return customSortedLinkers;
    }

    public void setCustomSortedLinkers(List<LinkedTargetEntryRecord> list) {
        System.out.println("[Menu] setCustomSortedLinkers aufgerufen mit: " + list.stream().map(e -> e.pos().toString()).toList());
        this.customSortedLinkers = list;
        setLinkers(list); // WICHTIG: aktualisiert GUI-Inhalte
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

        if (!customSortedLinkers.isEmpty()) {
            List<BlockPos> sorted = customSortedLinkers.stream()
                .map(LinkedTargetEntryRecord::pos)
                .toList();

            System.out.println("[Menu] removed called on thread: " + Thread.currentThread().getName());
            blockEntity.setSortedTargetPositions(sorted);
            System.out.println("[Menu] saved sortedTargetPositions on removed: " + sorted);

            if (!player.level().isClientSide) {
                ((ServerLevel)player.level()).sendBlockUpdated(blockEntity.getBlockPos(),
                    player.level().getBlockState(blockEntity.getBlockPos()),
                    player.level().getBlockState(blockEntity.getBlockPos()),
                    3);
                player.level().getChunk(blockEntity.getBlockPos()).setUnsaved(true);
            }
        } else {
            System.out.println("[Menu] removed called, aber keine customSortedLinkers zum Speichern");
        }
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
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (newDataAvailable) {
            setLinkers(latestLinkers);
            newDataAvailable = false;
        }
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