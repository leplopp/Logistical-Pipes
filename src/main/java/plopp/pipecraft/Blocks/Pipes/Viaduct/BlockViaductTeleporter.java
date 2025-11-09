package plopp.pipecraft.Blocks.Pipes.Viaduct;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterIdRegistry;
import plopp.pipecraft.logic.Connectable;
import plopp.pipecraft.logic.DimBlockPos;
import plopp.pipecraft.logic.Manager.ViaductTeleporterManager;
import plopp.pipecraft.logic.Travel.TravelData;
import plopp.pipecraft.logic.Travel.TravelSaveState;
import plopp.pipecraft.logic.Travel.TravelStop;
import plopp.pipecraft.logic.Travel.ViaductTravel;

public class BlockViaductTeleporter extends Block implements EntityBlock, Connectable {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public BlockViaductTeleporter(Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			BlockState neighborState = level.getBlockState(neighborPos);
			Block neighborBlock = neighborState.getBlock();

			if (neighborState.getBlock() instanceof BlockViaductLinker
					&& neighborState.hasProperty(BlockViaductLinker.FACING)) {

				Direction neighborFacing = neighborState.getValue(BlockViaductLinker.FACING);

				if (neighborPos.relative(neighborFacing).equals(pos)) {
					return null;
				}
			}

			if (neighborBlock instanceof BlockViaduct || neighborBlock instanceof BlockViaductLinker) {
				return this.defaultBlockState().setValue(FACING, dir);
			}
		}

		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide()) {

			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
				sp.openMenu(provider, buf -> buf.writeBlockPos(pos));
				return InteractionResult.CONSUME;
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		super.playerWillDestroy(level, pos, state, player);

		level.playSound(player, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
		level.playSound(player, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
		level.playSound(player, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
		return state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
	    if (placer instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
	        BlockEntity blockEntity = level.getBlockEntity(pos);
	        if (blockEntity instanceof BlockEntityViaductTeleporter teleporter) {
	            UUID ownerUUID = serverPlayer.getUUID();
	            teleporter.setOwnerUUID(ownerUUID);

	            DimBlockPos dimPos = new DimBlockPos(serverLevel.dimension(), pos);
	            TeleporterEntryRecord entry = new TeleporterEntryRecord(
	                dimPos,
	                teleporter.getStartEntry(),
	                teleporter.getGoalEntry(),
	                ownerUUID
	            );

	            ViaductTeleporterManager.setTeleport(serverLevel, dimPos, entry);
	        }
	    }
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
	    if (!state.is(newState.getBlock())) {
	        BlockEntity blockEntity = level.getBlockEntity(pos);
	        if (blockEntity instanceof BlockEntityViaductTeleporter teleporter
	                && level instanceof ServerLevel serverLevel) {

	            String targetId = teleporter.getTargetId();
	            if (!targetId.isEmpty()) {
	                ViaductTeleporterIdRegistry.unregisterTeleporter(targetId);
	            }

	            DimBlockPos dimPos = new DimBlockPos(serverLevel.dimension(), pos);
	            ViaductTeleporterManager.removeTeleport(serverLevel, dimPos);
	        }
	        super.onRemove(state, level, pos, newState, isMoving);
	    }
	}
/*
	@Override
	public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
	    ItemStack stack = new ItemStack(this.asItem());

	    if (state.hasProperty(BlockViaductTeleporter.COLOR)) {
	        DyedViaductItem.setColor(stack, state.getValue(BlockViaductTeleporter.COLOR));
	    }
	    popResource(world, pos, stack); 
	}*/
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
	    if (context instanceof EntityCollisionContext entityContext) {
	        Entity entity = entityContext.getEntity();
	        if (entity instanceof Player player) {
	            if (ViaductTravel.isTravelActive(player)) {
	                return Shapes.empty();
	            }
	        }
	    }

	    Direction facing = state.getValue(FACING);
	    VoxelShape finalShape;

	    switch (facing) {
	        case DOWN -> {
	            VoxelShape upArm = box(4, 12, 4, 12, 16, 12);
	            VoxelShape northArm = box(4, 0, 0, 12, 12, 16);
	            VoxelShape westArm = box(0, 0, 4, 16, 12, 12);
	            finalShape = Shapes.or(northArm, westArm, upArm);
	        }
	        case UP -> {
	            VoxelShape downArm = box(4, 0, 4, 12, 4, 12);
	            VoxelShape northArm = box(4, 4, 0, 12, 16, 16);
	            VoxelShape westArm = box(0, 4, 4, 16, 16, 12);
	            finalShape = Shapes.or(northArm, westArm, downArm);
	        }
	        case NORTH -> {
	            VoxelShape northArm = box(0, 4, 0, 16, 12, 12);
	            VoxelShape westArm = box(4, 0, 0, 12, 16, 12);
	            VoxelShape southArm = box(4, 4, 12, 12, 12, 16);
	            finalShape = Shapes.or(northArm, westArm, southArm);
	        }
	        case SOUTH -> {
	            VoxelShape northArm = box(4, 4, 0, 12, 12, 4);
	            VoxelShape westArm = box(4, 0, 4, 12, 16, 16);
	            VoxelShape southArm = box(0, 4, 4, 16, 12, 16);
	            finalShape = Shapes.or(northArm, westArm, southArm);
	        }
	        case EAST -> {
	            VoxelShape northArm = box(4, 4, 0, 16, 12, 16);
	            VoxelShape westArm = box(0, 4, 4, 4, 12, 12);
	            VoxelShape southArm = box(4, 0, 4, 16, 16, 12);
	            finalShape = Shapes.or(northArm, westArm, southArm);
	        }
	        case WEST -> {
	            VoxelShape eastArm = box(12, 4, 4, 16, 12, 12);
	            VoxelShape westArm = box(0, 4, 0, 12, 12, 16);
	            VoxelShape southArm = box(0, 0, 4, 12, 16, 12);
	            finalShape = Shapes.or(eastArm, westArm, southArm);
	        }
	        default -> {
	            VoxelShape northArm = box(4, 0, 0, 12, 16, 16);
	            VoxelShape westArm = box(0, 4, 0, 16, 12, 16);
	            finalShape = Shapes.or(northArm, westArm);
	        }
	    }

	    return finalShape;
	}
	
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
	    super.stepOn(level, pos, state, entity);

	    if (level.isClientSide()) return;
	    if (!(entity instanceof ServerPlayer player)) return;
	    if (!player.isShiftKeyDown()) return;

	    System.out.println("[Teleporter] Spieler erkannt: " + player.getName().getString());

	    BlockEntity be = level.getBlockEntity(pos);
	    if (!(be instanceof BlockEntityViaductTeleporter teleporter)) {
	        System.out.println("[Teleporter] -> Abbruch: Kein BlockEntityViaductTeleporter gefunden");
	        return;
	    }

	    if (player.getPersistentData().getBoolean("pipecraft_teleporting")) {
	        System.out.println("[Teleporter] -> Abbruch: Spieler ist bereits im Teleportvorgang");
	        return;
	    }

	    player.getPersistentData().putBoolean("pipecraft_teleporting", true);
	    System.out.println("[Teleporter] Teleport-Flag gesetzt, beginne Teleport...");

	    teleportPlayer(player, teleporter);

	    level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);
	    level.getServer().execute(() -> {
	        player.getPersistentData().putBoolean("pipecraft_teleporting", false);
	        System.out.println("[Teleporter] Teleport-Flag zurückgesetzt für Spieler " + player.getName().getString());
	    });
	}
	
	public void trigger(Level level, BlockPos linkerPos) {
	    if (level.isClientSide()) return;

	    for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
	        TravelData data = ViaductTravel.getTravelData(player);
	        if (data == null) continue;

	        if (!data.path.contains(new DimBlockPos(level.dimension(), linkerPos))) continue;

	        TravelStop.pauseAndHold(player);
	        data.allowTeleportWhilePaused = true; 

	        BlockEntity be = level.getBlockEntity(linkerPos);
	        if (be instanceof BlockEntityViaductTeleporter teleporter) {
	            teleportPlayer(player, teleporter);
	        } 
	    }
	}

	private void teleportPlayer(ServerPlayer player, BlockEntityViaductTeleporter startTeleporter) {
	    TravelData data = ViaductTravel.getTravelData(player);
	    if (data == null) return;
	    DimBlockPos targetDimPos = new DimBlockPos(
	        startTeleporter.getTargetDimension(),
	        startTeleporter.getTargetPosition()
	    );

	    if (targetDimPos.getPos() == null || targetDimPos.getPos().equals(BlockPos.ZERO)) {
	        return;
	    }

	    ServerLevel targetLevel = player.server.getLevel(targetDimPos.getDimension());
	    if (targetLevel == null) {
	        return;
	    }

	    Vec3 teleportPos = new Vec3(
	        targetDimPos.getPos().getX() + 0.5,
	        targetDimPos.getPos().getY() + 1.0,
	        targetDimPos.getPos().getZ() + 0.5
	    );

	    targetLevel.getChunk(targetDimPos.getPos());

	    TravelStop.pauseAndHold(player);

	    if (player.level() != targetLevel) {

	        DimensionTransition transition = new DimensionTransition(
	            targetLevel,
	            teleportPos,
	            Vec3.ZERO,
	            player.getYRot(),
	            player.getXRot(),
	            false,
	            entity -> {
	                if (entity instanceof ServerPlayer sp) {
	                    sp.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);

	                    TravelData d = ViaductTravel.getTravelData(sp);
	                    if (d != null) d.allowTeleportWhilePaused = false;
	                }
	            }
	        );
	        player.changeDimension(transition);
	    } else {

	        player.connection.teleport(
	            teleportPos.x,
	            teleportPos.y,
	            teleportPos.z,
	            player.getYRot(),
	            player.getXRot()
	        );

	        TravelData d = ViaductTravel.getTravelData(player);
	        if (d != null) d.allowTeleportWhilePaused = false;
	    }

	    TravelSaveState.resume(player);
	    data.triggeredTeleporters.clear();
	}
	
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext) {
			Entity entity = entityContext.getEntity();
			if (entity instanceof Player player) {
				if (ViaductTravel.isTravelActive(player)) {
					return Shapes.empty();
				}
			}
		}
		return getShape(state, world, pos, context);
	}

	@Override
	public boolean canConnectTo(BlockState state, Level level, BlockPos pos, Direction direction) {
		Direction facing = state.getValue(BlockStateProperties.FACING);
		return direction == facing;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityViaductTeleporter(pos, state);
	}
}