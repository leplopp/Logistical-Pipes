package com.plopp.pipecraft;

import org.lwjgl.glfw.GLFW;

import com.plopp.pipecraft.logic.ViaductTravel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class CommonEvents {


	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
	    Player player = event.getEntity();

	    if (!player.level().isClientSide()) {
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
	    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
	        Player player = event.getEntity();
	        ViaductTravel.stop(player); 
	  }
	    @SubscribeEvent
	    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
	        Player player = event.getEntity();
	        ViaductTravel.stop(player);
	    }

	    @SubscribeEvent
	    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
	        Player player = event.getEntity();
	        ViaductTravel.stop(player);
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

}
