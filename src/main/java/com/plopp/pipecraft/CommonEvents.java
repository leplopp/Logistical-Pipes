package com.plopp.pipecraft;

import java.util.List;
import java.util.UUID;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import com.plopp.pipecraft.logic.ViaductTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;

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

	    boolean isOnChargerBlock = state.getBlock() == BlockRegister.VIADUCTLINKER.get();
	    boolean isCharging = ViaductTravel.isCharging(id);

	    Vec3 playerPos = player.position();
	    Vec3 blockCenter = new Vec3(pos.getX() + 0.5, playerPos.y, pos.getZ() + 0.5);
	    double maxDistance = 0.5;

	    if (isOnChargerBlock && player.isShiftKeyDown()) {

	        boolean hasAdjacentCharger = false;
	        for (Direction dir : Direction.Plane.HORIZONTAL) {
	            BlockPos neighborPos = pos.relative(dir);
	            BlockState neighborState = player.level().getBlockState(neighborPos);
	            if (neighborState.getBlock() == BlockRegister.VIADUCTLINKER.get()) {
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
	            ViaductTravel.start(player, pos, 32); //change speed / 32 standart = 1 Block per second / min 1 / max 100
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

	             //player.setInvisible(true);
	             player.setInvulnerable(true);
	             player.setSwimming(false);
	             player.setPose(Pose.SLEEPING);
	             player.noPhysics = true;
	             player.setNoGravity(true);
	             player.setDeltaMovement(Vec3.ZERO);
	             ItemStack helmet = player.getInventory().armor.get(3);
                 ItemStack chestplate = player.getInventory().armor.get(2);
                 ItemStack leggings = player.getInventory().armor.get(1);
                 ItemStack boots = player.getInventory().armor.get(0);

                 ViaductTravel.storedArmor.put(player.getUUID(), List.of(helmet, chestplate, leggings, boots));

                 player.getInventory().armor.set(3, ItemStack.EMPTY);
                 player.getInventory().armor.set(2, ItemStack.EMPTY);
                 player.getInventory().armor.set(1, ItemStack.EMPTY);
                 player.getInventory().armor.set(0, ItemStack.EMPTY);
	         });
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
	            List<ItemStack> armor = ViaductTravel.storedArmor.remove(player.getUUID());
	            if (armor != null && armor.size() == 4) {
	                player.getInventory().armor.set(3, armor.get(0)); 
	                player.getInventory().armor.set(2, armor.get(1)); 
	                player.getInventory().armor.set(1, armor.get(2)); 
	                player.getInventory().armor.set(0, armor.get(3)); 
	            }
	        }
	    }
	    
	    @SubscribeEvent
	    public static void onClientTick(ClientTickEvent.Pre event) {
	        Minecraft mc = Minecraft.getInstance();
	        if (mc.player == null) return;

	        Player player = mc.player;

	        if (ViaductTravel.isTravelActive(player)) {
	            player.setDeltaMovement(Vec3.ZERO);

	            if (player instanceof LocalPlayer localPlayer) {
	                localPlayer.input.forwardImpulse = 0;
	                localPlayer.input.leftImpulse = 0;
	                localPlayer.input.jumping = false;
	                localPlayer.input.shiftKeyDown = false;
	                player.setOnGround(true);
	                
	                player.setPose(Pose.STANDING);
	                
	            }
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
	        public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
	            Player player = event.getEntity();
	            if (ViaductTravel.isTravelActive(player)) {
	                ItemStack item = event.getItemStack();
	                if (item.getItem() instanceof ArmorItem) {
	                    event.setCanceled(true);
	                }
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
