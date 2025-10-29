package plopp.pipecraft.Blocks.Pipes.Viaduct;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.gui.ViaductGuiProvider;
import plopp.pipecraft.logic.Connectable;
import plopp.pipecraft.logic.ViaductLinkerManager;
import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.model.ViaductBlockBox;

public class BlockViaductLinker extends Block implements EntityBlock, Connectable {

	public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
	public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
	public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");
	public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");
	public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
	public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
	public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");

	@SuppressWarnings("unchecked")
	@Nullable
	private static <T extends BlockEntity, E extends BlockEntity> BlockEntityTicker<T> createTickerHelper(
			BlockEntityType<T> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
		return expectedType == actualType ? (BlockEntityTicker<T>) ticker : null;
	}

	public BlockViaductLinker(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(CONNECTED_NORTH, false)
				.setValue(CONNECTED_SOUTH, false).setValue(CONNECTED_EAST, false).setValue(CONNECTED_WEST, false)
				.setValue(CONNECTED_UP, false).setValue(CONNECTED_DOWN, false).setValue(COLOR, DyeColor.WHITE)
				.setValue(TRANSPARENT, true).setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
			BlockPos pos, BlockPos neighborPos) {
		boolean connect = isViaduct(neighborState, (Level) level, neighborPos, direction);
		return state.setValue(getPropertyForDirection(direction), connect);
	}

	public BooleanProperty getPropertyForDirection(Direction direction) {
		return switch (direction) {
		case NORTH -> CONNECTED_NORTH;
		case EAST -> CONNECTED_EAST;
		case SOUTH -> CONNECTED_SOUTH;
		case WEST -> CONNECTED_WEST;
		case UP -> CONNECTED_UP;
		case DOWN -> CONNECTED_DOWN;

		default -> throw new IllegalArgumentException("Invalid direction for connection: " + direction);
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, CONNECTED_UP, CONNECTED_DOWN,
				FACING, COLOR, TRANSPARENT);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		return level.isClientSide ? null
				: createTickerHelper(type, BlockEntityRegister.VIADUCT_LINKER.get(),
						BlockEntityViaductLinker::serverTick);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction facing = context.getNearestLookingDirection().getOpposite();

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			BlockState neighborState = level.getBlockState(neighborPos);

			if (neighborState.getBlock() instanceof BlockViaductLinker) {
				return null;
			}
		}

		BlockPos frontPos = pos.relative(facing);
		BlockState frontState = level.getBlockState(frontPos);
		if (frontState.getBlock() instanceof BlockViaduct) {
			return null;
		}

		return this.defaultBlockState().setValue(FACING, facing);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide()) {
			if (player.isCrouching() && player instanceof ServerPlayer serverPlayer) {

				serverPlayer.openMenu(new ViaductGuiProvider(pos), buf -> buf.writeBlockPos(pos));
				return InteractionResult.CONSUME;
			} else {
				BlockEntity be = level.getBlockEntity(pos);
				if (be instanceof MenuProvider prov) {
					if (player instanceof ServerPlayer sp) {
						sp.openMenu(prov, buf -> buf.writeBlockPos(pos));
						return InteractionResult.CONSUME;
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);

		if (!level.isClientSide && level.getServer() != null) {
			level.getServer().execute(() -> {
				BlockEntity be = level.getBlockEntity(pos);
				if (be instanceof BlockEntityViaductLinker linker) {
					String name = linker.getCustomName();
					ItemStack icon = linker.getDisplayedItem();

					ViaductLinkerManager.addOrUpdateLinker(pos, name != null ? name : "Unnamed",
							icon != null ? icon : ItemStack.EMPTY);
				}
			});
		}
	}

	@Override
	public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!level.isClientSide && oldState.getBlock() != newState.getBlock()) {
			if (level.getServer() != null) {
				level.getServer().execute(() -> {
					ViaductLinkerManager.removeLinker(pos);
				});
			}
		}
		super.onRemove(oldState, level, pos, newState, isMoving);
	}

	public void updateConnections(Level level, BlockPos pos) {
		BlockState current = level.getBlockState(pos);
		if (!(current.getBlock() instanceof BlockViaductLinker))
			return;

		boolean north = isViaduct(level.getBlockState(pos.relative(Direction.NORTH)), level,pos.relative(Direction.NORTH), Direction.SOUTH);
		boolean south = isViaduct(level.getBlockState(pos.relative(Direction.SOUTH)), level,pos.relative(Direction.SOUTH), Direction.NORTH);
		boolean east = isViaduct(level.getBlockState(pos.relative(Direction.EAST)), level, pos.relative(Direction.EAST),Direction.WEST);
		boolean west = isViaduct(level.getBlockState(pos.relative(Direction.WEST)), level, pos.relative(Direction.WEST),Direction.EAST);
		boolean up = isViaduct(level.getBlockState(pos.relative(Direction.UP)), level, pos.relative(Direction.UP),Direction.DOWN);
		boolean down = isViaduct(level.getBlockState(pos.relative(Direction.DOWN)), level, pos.relative(Direction.DOWN),Direction.UP);

		BlockState updated = current.setValue(CONNECTED_NORTH, north).setValue(CONNECTED_SOUTH, south)
				.setValue(CONNECTED_EAST, east).setValue(CONNECTED_WEST, west).setValue(CONNECTED_UP, up)
				.setValue(CONNECTED_DOWN, down)
				.setValue(FACING, current.getValue(FACING));

		if (!current.equals(updated)) {
			level.setBlock(pos, updated, 3);
		}
	}

	public void updateAndPropagate(Level level, BlockPos pos, BlockState current, BlockState updated) {
		if (!current.equals(updated)) {
			level.setBlock(pos, updated, 3);

			for (Direction dir : Direction.values()) {
				BlockPos neighborPos = pos.relative(dir);
				BlockState neighborState = level.getBlockState(neighborPos);
				if (neighborState.getBlock() instanceof BlockViaductLinker) {
					updateConnections(level, neighborPos);
				}
			}
		}
	}

	private boolean isViaduct(BlockState state, Level level, BlockPos neighborPos, Direction toDirection) {
		if (state == null)
			return false;

		Block block = state.getBlock();

		if (block instanceof BlockViaduct || block instanceof BlockViaductLinker) {
			return true;
		}

		if (block instanceof BlockViaductDetector || block instanceof BlockViaductSpeed) {
			Direction detectorFacing = state.getValue(BlockStateProperties.FACING);

			return detectorFacing == toDirection || detectorFacing == toDirection.getOpposite();
		}

		if (block instanceof BlockViaductTeleporter) {

			Direction teleFacing = state.getValue(BlockViaductTeleporter.FACING);
			return teleFacing == toDirection.getOpposite();
		}
		return false;
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
		updateConnections(level, pos);
	}

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

		return ViaductBlockBox.getShape(state, CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST,
				CONNECTED_UP, CONNECTED_DOWN);
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityViaductLinker(pos, state);
	}

	@Override
	public boolean canConnectTo(BlockState state, Level level, BlockPos pos, Direction direction) {

		BlockPos neighborPos = pos.relative(direction);
		BlockState neighborState = level.getBlockState(neighborPos);
		Block neighborBlock = neighborState.getBlock();

		if (neighborBlock instanceof BlockViaduct || neighborBlock instanceof BlockViaductLinker) {
			return true;
		}

		if (neighborBlock instanceof BlockViaductDetector || neighborBlock instanceof BlockViaductSpeed) {
			Direction detectorFacing = neighborState.getValue(BlockStateProperties.FACING);
			return detectorFacing.getAxis() == direction.getAxis();
		}

		return false;
	}
}