package com.plopp.pipecraft.gui.viaductlinker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import com.plopp.pipecraft.Network.LinkedTargetEntryRecord;
import com.plopp.pipecraft.Network.NetworkHandler;
import com.plopp.pipecraft.Network.ViaductLinkerListPacket;
import com.plopp.pipecraft.gui.MenuTypeRegister;
import com.plopp.pipecraft.logic.LinkedTargetEntry;
import com.plopp.pipecraft.logic.ViaductLinkerManager;

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

public class ViaductLinkerMenu extends AbstractContainerMenu {

    public final BlockEntityViaductLinker blockEntity;
    public final List<Component> linkedNames = new ArrayList<>();
    public final List<ItemStack> linkedItems = new ArrayList<>();
    private List<LinkedTargetEntryRecord> linkers = new ArrayList<>();
    private boolean manualOverride = false;


    public ViaductLinkerMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
    	  this(containerId, inv, (BlockEntityViaductLinker) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ViaductLinkerMenu(int containerId, Inventory inv, BlockEntityViaductLinker tile) {
        super(MenuTypeRegister.VIADUCT_LINKER.get(), containerId);
        this.blockEntity = tile;
        
        if (!inv.player.level().isClientSide()) {
            // Serverseitig die Linkerliste holen und Packet an den Client senden
        	Set<BlockPos> connected = tile.getLinkedTargets().stream()
        		    .map(e -> e.pos)
        		    .collect(Collectors.toSet());

        		List<LinkedTargetEntryRecord> linkers = ViaductLinkerManager.getAllLinkersData().stream()
        		    .filter(e -> connected.contains(e.pos()))
        		    .toList();

        		ViaductLinkerListPacket packet = new ViaductLinkerListPacket(linkers);
        		NetworkHandler.sendToClient((ServerPlayer) inv.player, packet);
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
        for (BlockPos pos : linkedPositions) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlockEntityViaductLinker linker) {
                linkedNames.add(Component.literal(linker.getCustomName()));
                linkedItems.add(linker.getDisplayedItem());
            } else {
                linkedNames.add(Component.literal("Unknown"));
                linkedItems.add(ItemStack.EMPTY);
            }
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