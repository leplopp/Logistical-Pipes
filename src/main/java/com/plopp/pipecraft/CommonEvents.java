package com.plopp.pipecraft;

import com.plopp.pipecraft.logic.ViaductTravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.Entity;

@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class CommonEvents {


	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
	    Player player = event.getEntity();

	    if (!player.level().isClientSide()) {
	    	 if (player instanceof ServerPlayer serverPlayer) {
	             if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
	                 return;
	             }
	         }
	        BlockPos pos = player.blockPosition();
	        BlockState blockState = player.level().getBlockState(pos);

	        if (blockState.isAir()) {
	            pos = pos.below();
	            blockState = player.level().getBlockState(pos);
	        }

	        Block block = blockState.getBlock();

	        if (player.isShiftKeyDown() && block == Blocks.STONE && !ViaductTravel.isTravelActive(player)) {
	            ViaductTravel.start(player, pos);
	        }

	        ViaductTravel.tick(player);
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
	            ViaductTravel.stop(player);
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
	    @SubscribeEvent
	    public static void onMobTarget(LivingChangeTargetEvent event) {
	        if (event.getNewAboutToBeSetTarget() instanceof Player player) {
	            if (ViaductTravel.isTravelActive(player)) {
	                event.setCanceled(true); 
	            }
	        }
	    }

}
