package com.plopp.pipecraft;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import com.plopp.pipecraft.logic.ViaductTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;

@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class CommonEvents {	
	
	@SubscribeEvent
	public static void onClientPlayerTick(PlayerTickEvent.Post event) {
	    Player player = event.getEntity();
	    if (!(player instanceof LocalPlayer localPlayer)) return;

	    UUID id = localPlayer.getUUID();
	    if (ViaductTravel.shouldTriggerJump(id)) {
	        localPlayer.jumpFromGround();
	    }
	}
	
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
	    Player player = event.getEntity();

	    if (!(player instanceof ServerPlayer serverPlayer)) return;

	    if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
	        if (ViaductTravel.isTravelActive(player)) {
	            ViaductTravel.stop(player, false);
	        }
	        if (ViaductTravel.isCharging(player.getUUID())) {
	            ViaductTravel.cancelCharge(player);
	            serverPlayer.displayClientMessage(
	                Component.translatable("viaduct.travel.cancel.charging.spectator")
	                         .withStyle(ChatFormatting.RED), true);
	        }
	        return;
	    }

	    BlockPos pos = player.blockPosition();
	    BlockState state = player.level().getBlockState(pos);
	    if (state.isAir()) {
	        pos = pos.below();
	        state = player.level().getBlockState(pos);
	    }

	    UUID id = player.getUUID();

	    boolean isOnChargerBlock = state.getBlock() == BlockRegister.VIADUCTCHARGERBLOCK.get();
	    boolean isCharging = ViaductTravel.isCharging(id);

	    Vec3 playerPos = player.position();
	    Vec3 blockCenter = new Vec3(pos.getX() + 0.5, playerPos.y, pos.getZ() + 0.5);
	    double maxDistance = 0.5;

	    if (isOnChargerBlock && player.isShiftKeyDown()) {

	        boolean hasAdjacentCharger = false;
	        for (Direction dir : Direction.Plane.HORIZONTAL) {
	            BlockPos neighborPos = pos.relative(dir);
	            BlockState neighborState = player.level().getBlockState(neighborPos);
	            if (neighborState.getBlock() == BlockRegister.VIADUCTCHARGERBLOCK.get()) {
	                hasAdjacentCharger = true;
	                break;
	            }
	        }
	        if (hasAdjacentCharger) {
	            if (isCharging) {
	                ViaductTravel.cancelCharge(id);
	            }
	            return;
	        }

	        if (playerPos.distanceTo(blockCenter) > maxDistance) {
	            if (isCharging) {
	                ViaductTravel.cancelCharge(player);
	                serverPlayer.displayClientMessage(
	                    Component.translatable("viaduct.travel.cancel.charging.toofarfromblock")
	                             .withStyle(ChatFormatting.RED), true);
	            }
	            return;
	        }

	        BlockPos target = ViaductTravel.findTarget(player.level(), pos);
	        if (target == null || pos.distManhattan(target) <= 1) {
	            if (isCharging) {
	                ViaductTravel.cancelCharge(player);
	            }
	            return;
	        }

	        int current = ViaductTravel.incrementCharge(id);
	        int percent = Math.min(100, (int)((current / (float) ViaductTravel.MAX_CHARGE) * 100));
	        int last = ViaductTravel.getLastSentPercent(id);

	        if (percent != last) {
	            ViaductTravel.setLastSentPercent(id, percent);

	            int red = (int)(255 * (1 - percent / 100f));
	            int green = (int)(255 * (percent / 100f));
	            int colorInt = (red << 16) | (green << 8);

	            serverPlayer.displayClientMessage(
	                Component.translatable("viaduct.travel.charging." + percent + "%")
	                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorInt))),
	                true
	            );
	        }

	        if (current >= ViaductTravel.MAX_CHARGE) {
	            ViaductTravel.clearCharge(id);
	            ViaductTravel.start(player, pos, 32); //change speed / 16 standart = 1 Block per second / min 1 / max 100
	            serverPlayer.displayClientMessage(
	                Component.translatable("viaduct.travel.start")
	                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
	        }

	    } else {
	        if (isCharging) {
	            ViaductTravel.cancelCharge(player);
	            serverPlayer.displayClientMessage(
	                Component.translatable("viaduct.travel.cancel.charging")
	                    .withStyle(ChatFormatting.RED), true);
	        }
	    }

	    ViaductTravel.tick(player);
	}
	
	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
	    Level level = (Level) event.getLevel();
	    BlockPos pos = event.getPos();
	    BlockState state = level.getBlockState(pos);

	    if (state.getBlock() instanceof BlockViaduct) {
	        for (Player player : level.players()) {
	            UUID id = player.getUUID();

	            if (ViaductTravel.isCharging(id)) {
	                double dist = player.position().distanceTo(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
	                if (dist < 5.0) {
	                    ViaductTravel.cancelCharge(player);
	                    if (player instanceof ServerPlayer serverPlayer) {
	                        serverPlayer.displayClientMessage(
	                        		 Component.translatable("viaduct.travel.cancel.charging.brokenviaduct")
	                                     .withStyle(ChatFormatting.RED), true);
	                    }
	                }
	            }
	        }
	    }
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
	    Player player = event.getEntity();
	    UUID id = player.getUUID();

	    if (ViaductTravel.activeTravels.containsKey(id)) {
	    	 player.getServer().execute(() -> {

	             player.setInvisible(true);
	             player.setInvulnerable(true);
	             player.setSwimming(false);
	             player.setPose(Pose.STANDING);
	             player.noPhysics = true;
	             player.setNoGravity(true);
	         });
	    	
	        Level level = player.level();

	        if (!ViaductTravel.headEntities.containsKey(id)) {
	            ArmorStand stand = new ArmorStand(level, player.getX(), player.getY(), player.getZ());
	            stand.setInvisible(true);
	            stand.setInvulnerable(true);
	            stand.setNoGravity(true);
	            stand.setCustomName(player.getDisplayName());
	            stand.setCustomNameVisible(false);
	            stand.setSilent(true);
	            byte flags = 0;
	            flags |= ArmorStand.CLIENT_FLAG_MARKER;
	            flags |= ArmorStand.CLIENT_FLAG_NO_BASEPLATE;
	            stand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, flags);

	            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
	            CompoundTag skullOwner = new CompoundTag();

	            GameProfile profile = player.getGameProfile();

	            skullOwner.putUUID("Id", profile.getId());
	            skullOwner.putString("Name", profile.getName());

	            CompoundTag tag = new CompoundTag();
	            tag.put("SkullOwner", skullOwner);
	            head.getTags();

	            stand.setItemSlot(EquipmentSlot.HEAD, head);

	            level.addFreshEntity(stand);
	            ViaductTravel.headEntities.put(id, stand);
	        }
	    }
	}
    
	    @SubscribeEvent
	    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
	        Player player = event.getEntity();
	        UUID id = player.getUUID();
	        
	        if (ViaductTravel.activeTravels.containsKey(id)) {
	            player.setInvisible(false);
	            player.setInvulnerable(false);
	            player.setNoGravity(false);
	            player.noPhysics = false;
	        }

	        ArmorStand stand = ViaductTravel.headEntities.remove(id);
	        if (stand != null && !stand.level().isClientSide()) {
	            stand.remove(Entity.RemovalReason.DISCARDED);
	        }
	    }
	    
	    @SubscribeEvent
	    public static void onBlockBreak(PlayerEvent.BreakSpeed event) {
	        Player player = event.getEntity();
	        if (ViaductTravel.isTravelActive(player)) {
	            event.setCanceled(true);
	        }
	    }

	    @SubscribeEvent
	    public static void onBlockPlace(PlayerInteractEvent.RightClickBlock event) {
	        Player player = event.getEntity();
	        if (ViaductTravel.isTravelActive(player)) {
	            event.setCanceled(true);
	        }
	    }
	    @SubscribeEvent
	    public static void onBlockBreakAttempt(BlockEvent.BreakEvent event) {
	        Player player = event.getPlayer();
	        if (ViaductTravel.isTravelActive(player)) {
	            event.setCanceled(true); 
	        }
	    }
	    @SubscribeEvent
	    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
	        Player player = event.getEntity();
	        if (ViaductTravel.isTravelActive(player)) {
	            event.setCanceled(true);
	        }
	    }

	    @SubscribeEvent
	    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
	        Player player = event.getEntity();
	        if (ViaductTravel.isTravelActive(player)) {
	            event.setCanceled(true);
	        }
	    }
	    
	    @SubscribeEvent
	    public static void onGamemodeChange(PlayerChangeGameModeEvent event) {
	        Player player = event.getEntity();
	        
	        if (event.getNewGameMode() == GameType.SPECTATOR) {
	            ViaductTravel.stop(player, false);
	        }
	    }
	    
	    @SubscribeEvent
	    public static void onMobTarget(LivingChangeTargetEvent event) {
	        if (event.getNewAboutToBeSetTarget() instanceof Player player) {
	            if (ViaductTravel.isTravelActive(player)) {
	                event.setCanceled(true); 
	            }
	        }
	    }

	        @SubscribeEvent
	        public static void onAttackEntity(AttackEntityEvent event) {
	            Player player = event.getEntity();
	           
	            if (ViaductTravel.isTravelActive(player)) {
	                event.setCanceled(true); 
	        }
	    }

	    @SubscribeEvent
	    public static void onItemToss(ItemTossEvent event) {
	        Player player = event.getPlayer();

	        if (!(player instanceof ServerPlayer serverPlayer)) return;
	        if (!player.level().isClientSide() && ViaductTravel.isTravelActive(player)) {

	            ItemEntity entityItem = event.getEntity();
	            if (entityItem == null) return;

	            ItemStack stack = entityItem.getItem().copy();

	            event.setCanceled(true);

	            entityItem.remove(Entity.RemovalReason.DISCARDED);

	            if (!serverPlayer.getInventory().add(stack)) {
	                serverPlayer.drop(stack, false);
	            }

	            serverPlayer.displayClientMessage(
	            	    Component.translatable("viaduct.travel.drop_denied")
	            	        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
	            	    true
	            	);
	        }
	        
	  }
}
