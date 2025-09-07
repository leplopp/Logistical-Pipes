package plopp.pipecraft.Blocks.Pipes.Viaduct;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.logic.SpeedLevel;
import plopp.pipecraft.logic.SpeedManager;

public class BlockEntityViaductSpeed  extends  BlockEntity{
	  private static final Map<Item, Set<BlockEntityViaductSpeed>> REGISTRY = new ConcurrentHashMap<>();
	    private ItemStack idStack = ItemStack.EMPTY;
	    private static final Set<BlockEntityViaductSpeed> ALL_SPEED_BLOCKS = ConcurrentHashMap.newKeySet();

	    public BlockEntityViaductSpeed(BlockPos pos, BlockState state) {
	        super(BlockEntityRegister.VIADUCT_SPEED.get(), pos, state);
	        ALL_SPEED_BLOCKS.add(this);
	    }

	    public void setIdStack(ItemStack stack) {
	    	
	        if (!idStack.isEmpty() && idStack.getItem() instanceof BlockItem oldBlockItem) {
	            Set<BlockEntityViaductSpeed> set = REGISTRY.get(oldBlockItem);
	            if (set != null) set.remove(this);
	        }

	        this.idStack = stack.copy(); 
	        
	        if (stack.getItem() instanceof BlockItem newBlockItem) {
	            REGISTRY.computeIfAbsent(newBlockItem, k -> new HashSet<>()).add(this);
	        }

	        setChanged();
	        sendUpdatePacketToClients();
	    }

	    public void clearId() {
	        if (!idStack.isEmpty() && idStack.getItem() instanceof BlockItem oldBlockItem) {
	            Set<BlockEntityViaductSpeed> set = REGISTRY.get(oldBlockItem);
	            if (set != null) set.remove(this);
	        }

	        idStack = ItemStack.EMPTY;
	        setChanged();
	        sendUpdatePacketToClients();
	    }

	    public ItemStack getIdStack() {
	        return idStack.copy();
	    }

	    private void sendUpdatePacketToClients() {
	        if (level instanceof ServerLevel serverLevel) {
	            ClientboundBlockEntityDataPacket pkt = getUpdatePacket();
	            if (pkt != null) {
	                serverLevel.getPlayers(p -> p.blockPosition().closerThan(worldPosition, 64))
	                           .forEach(p -> p.connection.send(pkt));
	            }
	        }
	    }

	    @Override
	    public void setRemoved() {
	        super.setRemoved();
	        ALL_SPEED_BLOCKS.remove(this);
	        if (!idStack.isEmpty() && idStack.getItem() instanceof BlockItem oldBlockItem) {
	            Set<BlockEntityViaductSpeed> set = REGISTRY.get(oldBlockItem);
	            if (set != null) set.remove(this);
	        }
	    }

	    public static Set<BlockEntityViaductSpeed> getAll() {
	        return ALL_SPEED_BLOCKS;
	    }

	    public static Set<BlockEntityViaductSpeed> getByItem(Item item) {
	        return REGISTRY.getOrDefault(item, Set.of());
	    }

	    @Override
	    public void onLoad() {
	        super.onLoad();
	        SpeedLevel globalSpeed = SpeedManager.getSpeed(idStack);
	        if (globalSpeed != null) {
	            BlockState state = level.getBlockState(worldPosition);
	            if (state.getBlock() instanceof BlockViaductSpeed) {
	                level.setBlock(worldPosition, state.setValue(BlockViaductSpeed.SPEED, globalSpeed), 3);
	                setChanged();
	            }
	        }
	    }

	    @Override
	    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	        super.saveAdditional(tag, registries);
	        if (!idStack.isEmpty()) {
	            tag.put("IdStack", idStack.save(registries));
	        }
	    }

	    @Override
	    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	        super.loadAdditional(tag, registries);
	        if (tag.contains("IdStack", Tag.TAG_COMPOUND)) {
	            idStack = ItemStack.CODEC
	                    .parse(NbtOps.INSTANCE, tag.getCompound("IdStack"))
	                    .result()
	                    .orElse(ItemStack.EMPTY);

	            if (!idStack.isEmpty() && idStack.getItem() instanceof BlockItem blockItem) {
	                REGISTRY.computeIfAbsent(blockItem, k -> new HashSet<>()).add(this);
	            }
	        } else {
	            idStack = ItemStack.EMPTY;
	        }
	    }

	    @Override
	    public ClientboundBlockEntityDataPacket getUpdatePacket() {
	        return ClientboundBlockEntityDataPacket.create(this);
	    }

	    @Override
	    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
	        CompoundTag tag = new CompoundTag();
	        saveAdditional(tag, provider);
	        return tag;
	    }

	    @Override
	    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
	        loadAdditional(tag, provider);
	    }
	}
