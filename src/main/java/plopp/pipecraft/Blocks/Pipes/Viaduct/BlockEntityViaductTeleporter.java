package plopp.pipecraft.Blocks.Pipes.Viaduct;

import java.lang.reflect.Method;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.Network.data.DataEntryRecord;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterIdRegistry;
import plopp.pipecraft.gui.teleporter.ViaductTeleporterMenu;

public class BlockEntityViaductTeleporter extends BlockEntity implements MenuProvider {

	private UUID ownerUUID;
	private String customName = "teleporter";
	private boolean teleportIdVisible = false;
	private String startName = "";
	private String targetName = "";
	private String targetId = "";
	private ItemStack displayedItem = new ItemStack(Items.BARRIER);
	private ItemStack targetDisplayedItem = new ItemStack(Items.ENDER_PEARL);
	private ResourceKey<Level> targetDimension = Level.OVERWORLD;
	private BlockPos targetPosition = BlockPos.ZERO;

	public BlockEntityViaductTeleporter(BlockPos pos, BlockState state) {
		super(BlockEntityRegister.VIADUCT_TELEPORTER.get(), pos, state);
	}

	public String getCustomName() {
		return (customName == null || customName.isEmpty()) ? "teleporter" : customName;
	}

	public void setCustomName(String name) {
		this.customName = (name == null || name.isEmpty()) ? "teleporter" : name;
		setChanged();
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public void setTeleportIdVisible(boolean visible) {
		this.teleportIdVisible = visible;
		setChanged();
	}

	public boolean isTeleportIdVisible() {
		return teleportIdVisible;
	}

	public ResourceKey<Level> getTargetDimension() {
		return targetDimension;
	}

	public BlockPos getTargetPosition() {
		return targetPosition;
	}

	public void setTargetDimension(ResourceKey<Level> dimension) {
		this.targetDimension = dimension;
		setChanged();
	}

	public void setTargetPosition(BlockPos pos) {
		this.targetPosition = pos;
		setChanged();
	}

	public String getStartName() {
		return (startName == null || startName.isEmpty()) ? "" : startName;
	}

	public void setOwnerUUID(UUID uuid) {
		this.ownerUUID = uuid;
	}

	public void setStartName(String name) {
		this.startName = (name == null) ? "" : name;
		setChanged();
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public String getTargetName() {
		return (targetName == null || targetName.isEmpty()) ? "" : targetName;
	}

	public void setTargetName(String name) {
		this.targetName = (name == null) ? "" : name;
		setChanged();
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public String getTargetId() {
		return (targetId == null || targetId.isEmpty()) ? "" : targetId;
	}

	public void setTargetId(String id) {
		this.targetId = (id == null) ? "" : id;
		setChanged();
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public ItemStack getDisplayedItem() {
		return displayedItem != null ? displayedItem : ItemStack.EMPTY;
	}

	@Override
	public void setRemoved() {
	    if (!this.targetId.isEmpty()) {
	        ViaductTeleporterIdRegistry.unregisterTeleporter(this.targetId);
	        System.out.println("[Teleporter] Entfernt aus Registry: " + this.targetId);
	    }
	    super.setRemoved();
	}
	
	public ItemStack getTargetDisplayedItem() {
		return targetDisplayedItem != null ? targetDisplayedItem : ItemStack.EMPTY;
	}

	public void setTargetDisplayedItem(ItemStack stack) {
	    this.targetDisplayedItem = stack.copy();
	    setChanged();

	    if (level instanceof ServerLevel serverLevel) {
	        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

	        // --- Ziel-Teleporter suchen ---
	        String targetKey = BlockEntityViaductTeleporter.generateItemId(stack);
	        BlockPos foundPos = ViaductTeleporterIdRegistry.getPositionForId(targetKey);
	        if (foundPos != null) {
	            this.targetPosition = foundPos;
	            this.targetDimension = serverLevel.dimension(); // falls Dimension in Registry: ersetzen
	            PipeCraftIndex.LOGGER.info("[Teleporter] Ziel für '{}' gefunden: {}", targetKey, foundPos);
	        } else {
	            this.targetPosition = BlockPos.ZERO;
	            PipeCraftIndex.LOGGER.warn("[Teleporter] Kein Ziel-Teleporter für '{}' gefunden!", targetKey);
	        }
	    }
	}

	public static String generateItemId(ItemStack stack) {
	    if (stack.isEmpty()) return "";
	    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	    int damage = stack.getDamageValue();
	    CompoundTag tag = null;
	    try {
	        Method method = ItemStack.class.getDeclaredMethod("getTag");
	        tag = (CompoundTag) method.invoke(stack);
	    } catch (Exception ignored) {}
	    String tagString = (tag != null) ? tag.toString() : "";
	    return itemId + "#" + damage + "#" + tagString;
	}

	public void setDisplayedItem(ItemStack stack) {
	    if (level == null || level.isClientSide) {
	        this.displayedItem = stack.copy();
	        return;
	    }

	    String newId = generateItemId(stack);

	    // ✅ bereits bekanntes Item? Kein erneutes Registrieren
	    if (newId.equals(this.targetId)) {
	        this.displayedItem = stack.copy();
	        return;
	    }

	    // ✅ erst alte ID deregistrieren (z. B. beim Austausch)
	    if (!this.targetId.isEmpty()) {
	        ViaductTeleporterIdRegistry.unregisterTeleporter(this.targetId);
	    }

	    if (!stack.isEmpty()) {
	        String newId1 = generateItemId(stack);

	        if (ViaductTeleporterIdRegistry.isIdTaken(newId1)) {
	            PipeCraftIndex.LOGGER.warn("Teleporter-ID '{}' ist bereits vergeben!", newId1);
	            return;
	        }

	        // ✅ korrektes Registrieren
	        TeleporterEntryRecord record = new TeleporterEntryRecord(worldPosition, getStartEntry(), getGoalEntry(), ownerUUID);
	        ViaductTeleporterIdRegistry.registerTeleporter(newId1, record);
	        this.targetId = newId1;
	        PipeCraftIndex.LOGGER.info("[Teleporter] Registriert '{}' @ {}", newId1, worldPosition);

	        // ✅ direkt nach Ziel suchen
	        TeleporterEntryRecord targetRecord = ViaductTeleporterIdRegistry.getRecordById(newId1);
	        if (targetRecord != null) {
	            this.targetPosition = targetRecord.pos();
	            PipeCraftIndex.LOGGER.info("[Teleporter] Ziel für '{}' gefunden: {}", newId1, targetPosition);
	        } else {
	            this.targetPosition = BlockPos.ZERO;
	            PipeCraftIndex.LOGGER.warn("[Teleporter] Kein Ziel-Teleporter für '{}' gefunden!", newId1);
	        }
	    }

	    this.displayedItem = stack.copy();
	    setChanged();

	    if (level instanceof ServerLevel serverLevel) {
	        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	    }
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("screen.pipecraft.viaduct_teleporter");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new ViaductTeleporterMenu(id, inv, this);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);

		if (ownerUUID != null) {
			tag.putUUID("Owner", ownerUUID);
		}
		tag.putString("TargetDimension", targetDimension.location().toString());
		tag.putInt("TargetPosX", targetPosition.getX());
		tag.putInt("TargetPosY", targetPosition.getY());
		tag.putInt("TargetPosZ", targetPosition.getZ());
		tag.putBoolean("TeleportIdVisible", teleportIdVisible);
		tag.putString("CustomName", customName);
		tag.putString("StartName", startName);
		tag.putString("TargetName", targetName);
		tag.putString("TargetId", targetId);

		if (!displayedItem.isEmpty()) {
			tag.put("DisplayedItem", displayedItem.save(registries));
		}
		if (!targetDisplayedItem.isEmpty()) {
			tag.put("TargetDisplayedItem", targetDisplayedItem.save(registries));
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);

		teleportIdVisible = tag.getBoolean("TeleportIdVisible");

		if (tag.contains("TargetDimension", Tag.TAG_STRING)) {
			String dimString = tag.getString("TargetDimension");

			ResourceLocation dimensionRegistryRL = ResourceLocation.tryParse("minecraft:dimension");
			if (dimensionRegistryRL == null) {
				throw new IllegalStateException("ResourceLocation minecraft:dimension konnte nicht erzeugt werden.");
			}
			ResourceKey<Registry<Level>> dimensionRegistryKey = ResourceKey.createRegistryKey(dimensionRegistryRL);

			ResourceLocation dimensionRL = ResourceLocation.tryParse(dimString);
			if (dimensionRL != null) {
				this.targetDimension = ResourceKey.create(dimensionRegistryKey, dimensionRL);
			} else {
				this.targetDimension = Level.OVERWORLD;
			}
		}

		if (tag.contains("TargetPosX", Tag.TAG_INT) && tag.contains("TargetPosY", Tag.TAG_INT)
				&& tag.contains("TargetPosZ", Tag.TAG_INT)) {
			this.targetPosition = new BlockPos(tag.getInt("TargetPosX"), tag.getInt("TargetPosY"),
					tag.getInt("TargetPosZ"));
		}

		if (tag.hasUUID("Owner")) {
			this.ownerUUID = tag.getUUID("Owner");
		}

		if (tag.contains("CustomName", Tag.TAG_STRING)) {
			customName = tag.getString("CustomName");
		} else {
			customName = "teleporter";
		}

		if (tag.contains("StartName", Tag.TAG_STRING)) {
			startName = tag.getString("StartName");
		} else {
			startName = "";
		}

		if (tag.contains("TargetName", Tag.TAG_STRING)) {
			targetName = tag.getString("TargetName");
		} else {
			targetName = "";
		}

		if (tag.contains("TargetId", Tag.TAG_STRING)) {
			targetId = tag.getString("TargetId");
		} else {
			targetId = "";
		}

		if (tag.contains("DisplayedItem", Tag.TAG_COMPOUND)) {
			displayedItem = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("DisplayedItem")).result()
					.orElse(ItemStack.EMPTY);
		} else {
			displayedItem = ItemStack.EMPTY;
		}

		if (tag.contains("TargetDisplayedItem", Tag.TAG_COMPOUND)) {
			targetDisplayedItem = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("TargetDisplayedItem"))
					.result().orElse(ItemStack.EMPTY);
		} else {
			targetDisplayedItem = ItemStack.EMPTY;
		}
	}

	public DataEntryRecord getStartEntry() {
		return new DataEntryRecord(getBlockPos(), startName, displayedItem);
	}

	public DataEntryRecord getGoalEntry() {
		return new DataEntryRecord(getBlockPos(), targetName, targetDisplayedItem);
	}

	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
		return saveWithFullMetadata(lookup);
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
		this.loadAdditional(tag, lookup);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}