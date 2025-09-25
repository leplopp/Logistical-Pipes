package plopp.pipecraft.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductSpeed;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.logic.pipe.PipeTravel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class CommonEvents {	
	
	public static final Map<UUID, BlockPos> brushingPlayers = new HashMap<>();
	
	/*@SubscribeEvent+
	public static void onLevelTick(LevelTickEvent.Post event) {
	    if (!(event.getLevel() instanceof ServerLevel level)) return;
	    PipeTravel.tick(level.getLevel());

	    }
	*/
	
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
	    MinecraftServer server = event.getServer();

	    for (ServerLevel level : server.getAllLevels()) {
	        if (level == null) continue;
	        
	        PipeTravel.tick(level.getLevel());
	        
	        for (ServerPlayer player : level.players()) {
	            if (ViaductTravel.isTravelActive(player)) {
	                // Spieler-Reise-Tick
	            }
	        }
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
	        return;
	    }

	    ViaductTravel.tick(player);
	    
	    UUID uuid = serverPlayer.getUUID();
	    if (!brushingPlayers.containsKey(uuid)) return;

	    ItemStack stack = serverPlayer.getMainHandItem();
	    if (stack.getItem() != Items.BRUSH) {
	        brushingPlayers.remove(uuid);
	        return;
	    }

	    BlockPos pos = brushingPlayers.get(uuid);
	    Level level = serverPlayer.level();
	    BlockState state = level.getBlockState(pos);

	    if (!(state.getBlock() instanceof BlockViaduct)) {
	        brushingPlayers.remove(uuid); 
	        return;
	    }

	    int currentLevel = state.getValue(BlockViaduct.LIGHT_LEVEL);
	    if (currentLevel <= 0) {
	        brushingPlayers.remove(uuid); 
	        return;
	    }

	    BlockState newState = state.setValue(BlockViaduct.LIGHT_LEVEL, 0);
	    level.setBlock(pos, newState, 3);

	    ItemStack glowstoneReturn = new ItemStack(Items.GLOWSTONE_DUST, currentLevel);
	    boolean added = serverPlayer.getInventory().add(glowstoneReturn);
	    if (!added) serverPlayer.drop(glowstoneReturn, false);

	    serverPlayer.displayClientMessage(
	        Component.translatable("viaduct.lightlevel.brush" + currentLevel),
	        true
	    );
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
	    Player joiningPlayer = event.getEntity();
	    MinecraftServer server = joiningPlayer.getServer();
	    if (!(joiningPlayer instanceof ServerPlayer serverJoiningPlayer) || server == null) return;

	    if (ViaductTravel.activeTravels.containsKey(joiningPlayer.getUUID())) {
	        server.execute(() -> {
	            joiningPlayer.setInvulnerable(true);
	            joiningPlayer.setSwimming(false);
	            joiningPlayer.noPhysics = true;
	            joiningPlayer.setNoGravity(true);
	            joiningPlayer.setDeltaMovement(Vec3.ZERO);

	            ViaductTravel.resume(joiningPlayer);

	            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
	                if (!other.getUUID().equals(joiningPlayer.getUUID())) {
	                    NetworkHandler.sendTravelStateToAll(other, false);
	                }
	            }
	        });
	    }

	    server.execute(() -> {
	        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
	            if (!other.getUUID().equals(joiningPlayer.getUUID())
	                && ViaductTravel.isTravelActive(other)) {
	                NetworkHandler.sendTravelStateToAll(serverJoiningPlayer, false);
	            }
	        }
	    });
	}
    
	    @SubscribeEvent
	    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
	        Player player = event.getEntity();
	        UUID id = player.getUUID();
	        
	        if (ViaductTravel.activeTravels.containsKey(id)) {
	            player.setInvulnerable(false);
	            player.setNoGravity(false);
	            player.noPhysics = false;
	           
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

	    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
	        if (!(event.getEntity() instanceof Player player)) return;

	        if (ViaductTravel.isTravelActive(player)) {
	            event.setCanceled(true);
	            event.setCancellationResult(InteractionResult.FAIL);
	            return;
	        }

	        Level level = event.getLevel();
	        BlockPos pos = event.getPos();
	        InteractionHand hand = event.getHand();
	        ItemStack stack = player.getItemInHand(hand);
	        BlockState state = level.getBlockState(pos);
	        Block block = state.getBlock();  

	        boolean isViaduct = block instanceof BlockViaduct;
	        boolean isLinker = block instanceof BlockViaductLinker;
	        boolean isSpeed   = block instanceof BlockViaductSpeed;
	        boolean isDetector   = block instanceof BlockViaductDetector;

	        if ((isViaduct && state.hasProperty(BlockViaduct.TRANSPARENT)) ||
	        	    (isLinker && state.hasProperty(BlockViaductLinker.TRANSPARENT)) ||
	        	    (isSpeed   && state.hasProperty(BlockViaductSpeed.TRANSPARENT))||
	        	    (isDetector   && state.hasProperty(BlockViaductDetector.TRANSPARENT))) {

	   
	        	 if (isSpeed && player.isShiftKeyDown()) {
	        		 
	        	 }else {
	        	    if (stack.is(ItemTags.WOOL)) {
	        	        BlockState newState = state.setValue(
	        	            isViaduct ? BlockViaduct.TRANSPARENT :
	        	            isLinker ? BlockViaductLinker.TRANSPARENT :
	        	            isSpeed ?        BlockViaductSpeed.TRANSPARENT :
	        	            BlockViaductDetector.TRANSPARENT,
	        	            false
	        	        );
	        	        level.setBlock(pos, newState, 3);
	        	        player.displayClientMessage(Component.translatable("viaduct.transparency.deaktivated"), true);
	        	        event.setCancellationResult(InteractionResult.SUCCESS);
	        	        event.setCanceled(true);
	        	        return;
	        	    }

	        	    if (stack.is(Items.GLASS)) {
	        	    	BlockState newState = state.setValue(
		        	            isViaduct ? BlockViaduct.TRANSPARENT :
		        	            isLinker ? BlockViaductLinker.TRANSPARENT :
		        	            isSpeed ?        BlockViaductSpeed.TRANSPARENT :
		        	            BlockViaductDetector.TRANSPARENT,
		        	            true
	        	        );
	        	        level.setBlock(pos, newState, 3);
	        	        player.displayClientMessage(Component.translatable("viaduct.transparency.aktivated"), true);
	        	        event.setCancellationResult(InteractionResult.SUCCESS);
	        	        event.setCanceled(true);
	        	        return;
	        	    }
	        	 }
	        	}
	        
	        if (stack.getItem() instanceof DyeItem dyeItem) {
	            DyeColor clickedColor = dyeItem.getDyeColor();

	            if (player.isShiftKeyDown() && isViaduct) {
	                int maxBlocks = stack.getCount();
	                Set<BlockPos> visited = new HashSet<>();
	                Queue<BlockPos> queue = new ArrayDeque<>();
	                int colored = 0;

	                queue.add(pos);
	                visited.add(pos);

	                while (!queue.isEmpty() && colored < maxBlocks) {
	                    BlockPos currentPos = queue.poll();
	                    BlockState currentState = level.getBlockState(currentPos);

	                    if (!(currentState.getBlock() instanceof BlockViaduct)) continue;
	                    if (!currentState.hasProperty(BlockViaduct.COLOR)) continue;
	                    if (currentState.getValue(BlockViaduct.COLOR) == clickedColor) continue;

	                    level.setBlock(currentPos, currentState.setValue(BlockViaduct.COLOR, clickedColor), 3);
	                    colored++;

	                    for (Direction dir : Direction.values()) {
	                        BlockPos neighbor = currentPos.relative(dir);
	                        if (!visited.contains(neighbor)) {
	                            BlockState neighborState = level.getBlockState(neighbor);
	                            if (neighborState.getBlock() instanceof BlockViaduct &&
	                                neighborState.hasProperty(BlockViaduct.COLOR) &&
	                                neighborState.getValue(BlockViaduct.COLOR) != clickedColor) {

	                                visited.add(neighbor);
	                                queue.add(neighbor);
	                            }
	                        }
	                    }
	                }

	                if (colored > 0 && !player.isCreative()) {
	                    stack.shrink(colored);
	                }

	                player.displayClientMessage(
	                    Component.translatable(colored + "viaduct.multiple.color_change" + clickedColor.getName()),
	                    true
	                );
	                event.setCancellationResult(InteractionResult.SUCCESS);
	                event.setCanceled(true);
	                return;
	            }
	            
	            if (isSpeed && player.isShiftKeyDown()) {
	            } else {
	            if ((isViaduct && state.hasProperty(BlockViaduct.COLOR)) ||
	                    (isLinker && state.hasProperty(BlockViaductLinker.COLOR)) ||
	                    (isSpeed   && state.hasProperty(BlockViaductSpeed.COLOR))||
	                    (isDetector   && state.hasProperty(BlockViaductDetector.COLOR))) {

	                DyeColor currentColor;
	                BlockState newState;

	                if (isViaduct) {
	                    currentColor = state.getValue(BlockViaduct.COLOR);
	                    if (currentColor != clickedColor) {
	                        newState = state.setValue(BlockViaduct.COLOR, clickedColor);
	                    } else {
	                        player.displayClientMessage(Component.translatable("viaduct.color_change.already" + clickedColor.getName() + "."), true);
	                        event.setCancellationResult(InteractionResult.FAIL);
	                        event.setCanceled(true);
	                        return;
	                    }
	                } else if (isLinker) {
	                    currentColor = state.getValue(BlockViaductLinker.COLOR);
	                    if (currentColor != clickedColor) {
	                        newState = state.setValue(BlockViaductLinker.COLOR, clickedColor);
	                    } else {
	                        player.displayClientMessage(Component.translatable("viaduct.color_change.already" + clickedColor.getName() + "."), true);
	                        event.setCancellationResult(InteractionResult.FAIL);
	                        event.setCanceled(true);
	                        return;
	                    }
	                } else if (isSpeed){ 
	                    currentColor = state.getValue(BlockViaductSpeed.COLOR);
	                    if (currentColor != clickedColor) {
	                        newState = state.setValue(BlockViaductSpeed.COLOR, clickedColor);
	                    } else {
	                        player.displayClientMessage(Component.translatable("viaduct.color_change.already" + clickedColor.getName() + "."), true);
	                        event.setCancellationResult(InteractionResult.FAIL);
	                        event.setCanceled(true);
	                        return;
	                    }
	                } else { 
	                    currentColor = state.getValue(BlockViaductDetector.COLOR);
	                    if (currentColor != clickedColor) {
	                        newState = state.setValue(BlockViaductDetector.COLOR, clickedColor);
	                    } else {
	                        player.displayClientMessage(Component.translatable("viaduct.color_change.already"+ clickedColor.getName() + "."), true);
	                        event.setCancellationResult(InteractionResult.FAIL);
	                        event.setCanceled(true);
	                        return;
	                    }
	                }

	                level.setBlock(pos, newState, 3);
	                if (!player.isCreative()) stack.shrink(1);
	                player.displayClientMessage(Component.translatable("viaduct.color_change" + clickedColor.getName()), true);
	                event.setCancellationResult(InteractionResult.SUCCESS);
	                event.setCanceled(true);
	            }
	        }
	        }

	        if (isViaduct && stack.getItem() == Items.GLOWSTONE_DUST) {
	            int currentLevel = state.getValue(BlockViaduct.LIGHT_LEVEL);
	            if (player.isShiftKeyDown()) {
	                if (currentLevel < 15) {
	                    int toConsume = 1;
	                    BlockState newState = state.setValue(BlockViaduct.LIGHT_LEVEL, currentLevel + toConsume);
	                    level.setBlock(pos, newState, 3);
	                    if (!player.isCreative()) stack.shrink(toConsume);
	                    player.displayClientMessage(Component.translatable("viaduct.light_level.increased" + (currentLevel + toConsume)), true);
	                    event.setCancellationResult(InteractionResult.SUCCESS);
	                    event.setCanceled(true);
	                    return;
	                } else {
	                    player.displayClientMessage(Component.translatable("viaduct.light_level.maximum"), true);
	                    event.setCancellationResult(InteractionResult.FAIL);
	                    event.setCanceled(true);
	                    return;
	                }
	            } else {
	                if (currentLevel < 15) {
	                    int maxIncrease = 15 - currentLevel;
	                    int toConsume = Math.min(stack.getCount(), maxIncrease);
	                    int newLevel = currentLevel + toConsume;
	                    BlockState newState = state.setValue(BlockViaduct.LIGHT_LEVEL, newLevel);
	                    level.setBlock(pos, newState, 3);
	                    if (!player.isCreative()) stack.shrink(toConsume);
	                    player.displayClientMessage(Component.translatable("viaduct.light_level.increased"  + newLevel), true);
	                    event.setCancellationResult(InteractionResult.SUCCESS);
	                    event.setCanceled(true);
	                    return;
	                } else {
	                    player.displayClientMessage(Component.translatable("viaduct.light_level.maximum"), true);
	                    event.setCancellationResult(InteractionResult.FAIL);
	                    event.setCanceled(true);
	                    return;
	                }
	            } 
	            
	        }
	        if (stack.getItem() == Items.BRUSH && state.getBlock() instanceof BlockViaduct) {
	            brushingPlayers.put(player.getUUID(), pos);
	            event.setCanceled(true);
	            event.setCancellationResult(InteractionResult.SUCCESS);
	        }

	
	        if (state.getBlock() instanceof BlockViaductSpeed && player.isShiftKeyDown()) {
	            BlockEntity be = level.getBlockEntity(pos);
	            if (be instanceof BlockEntityViaductSpeed speedBE) {
	                ItemStack held = player.getMainHandItem();

	                if (!held.isEmpty()) {
	                    ItemStack copy = held.copy();
	                    copy.setCount(1);
	                    speedBE.setIdStack(copy);
	                } else {
	                    speedBE.setIdStack(ItemStack.EMPTY);
	                }
	                speedBE.setChanged();

	                if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
	                    ClientboundBlockEntityDataPacket pkt = speedBE.getUpdatePacket();
	                    if (pkt != null) {
	                        serverLevel.getPlayers(p -> p.blockPosition().closerThan(pos, 64))
	                                   .forEach(sp -> sp.connection.send(pkt));
	                    }
	                }

	                event.setCanceled(true);
	                event.setCancellationResult(InteractionResult.SUCCESS);
	            }
	        
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