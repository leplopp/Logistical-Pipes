package plopp.pipecraft.Blocks.Pipes.Viaduct;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;
import plopp.pipecraft.logic.Connectable;
import plopp.pipecraft.logic.Travel.ViaductTravel;
import plopp.pipecraft.model.ViaductBlockBox;

public class BlockViaduct extends Block implements Connectable {

	public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
	public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
	public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");
	public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");
	public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
	public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");
	public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);
	public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
	public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");

	public BlockViaduct(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(CONNECTED_NORTH, false)
				.setValue(CONNECTED_SOUTH, false).setValue(CONNECTED_EAST, false).setValue(CONNECTED_WEST, false)
				.setValue(CONNECTED_UP, false).setValue(CONNECTED_DOWN, false).setValue(COLOR, DyeColor.WHITE)
				.setValue(TRANSPARENT, true).setValue(LIGHT_LEVEL, 0));
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
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (stack.is(ItemTags.WOOL) || stack.is(Items.GLASS)) {
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, CONNECTED_UP, CONNECTED_DOWN,
				LIGHT_LEVEL, COLOR, TRANSPARENT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			BlockState neighborState = level.getBlockState(neighborPos);

			if (neighborState.getBlock() instanceof BlockViaductLinker
					&& neighborState.hasProperty(BlockViaductLinker.FACING)) {

				Direction neighborFacing = neighborState.getValue(BlockViaductLinker.FACING);

				if (neighborPos.relative(neighborFacing).equals(pos)) {
					return null;
				}
			}
		}
		return this.defaultBlockState();
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (!level.isClientSide) {
			updateConnections(level, pos);
			for (Direction dir : Direction.values()) {
				updateConnections(level, pos.relative(dir));
			}
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!level.isClientSide && state.getBlock() != newState.getBlock()) {

			for (Direction dir : Direction.Plane.HORIZONTAL) {
				updateConnections(level, pos.relative(dir));
			}
		}

		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
	    ItemStack stack = new ItemStack(this.asItem());

	    if (state.hasProperty(BlockViaduct.COLOR)) {
	        DyedViaductItem.setColor(stack, state.getValue(BlockViaduct.COLOR));
	    }
	    popResource(world, pos, stack); 
	}
	
	public void updateConnections(Level level, BlockPos pos) {
		BlockState current = level.getBlockState(pos);
		if (!(current.getBlock() instanceof BlockViaduct))
			return;

		boolean north = isViaduct(level.getBlockState(pos.relative(Direction.NORTH)), level,
				pos.relative(Direction.NORTH), Direction.SOUTH);
		boolean south = isViaduct(level.getBlockState(pos.relative(Direction.SOUTH)), level,
				pos.relative(Direction.SOUTH), Direction.NORTH);
		boolean east = isViaduct(level.getBlockState(pos.relative(Direction.EAST)), level, pos.relative(Direction.EAST),
				Direction.WEST);
		boolean west = isViaduct(level.getBlockState(pos.relative(Direction.WEST)), level, pos.relative(Direction.WEST),
				Direction.EAST);
		boolean up = isViaduct(level.getBlockState(pos.relative(Direction.UP)), level, pos.relative(Direction.UP),
				Direction.DOWN);
		boolean down = isViaduct(level.getBlockState(pos.relative(Direction.DOWN)), level, pos.relative(Direction.DOWN),
				Direction.UP);

		BlockState updated = current.setValue(CONNECTED_NORTH, north).setValue(CONNECTED_SOUTH, south)
				.setValue(CONNECTED_EAST, east).setValue(CONNECTED_WEST, west).setValue(CONNECTED_UP, up)
				.setValue(CONNECTED_DOWN, down);

		if (!current.equals(updated)) {
			updateAndPropagate(level, pos, current, updated);
		}
	}

	public void updateAndPropagate(Level level, BlockPos pos, BlockState current, BlockState updated) {
		level.setBlock(pos, updated, 3);

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			BlockState neighborState = level.getBlockState(neighborPos);
			if (neighborState.getBlock() instanceof BlockViaduct) {
				updateConnections(level, neighborPos);
			}
		}
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
			BlockPos pos, BlockPos neighborPos) {
		boolean connect = isViaduct(neighborState, (Level) level, neighborPos, direction);
		return state.setValue(getPropertyForDirection(direction), connect);
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
		return state.getValue(LIGHT_LEVEL);
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
			return teleFacing == toDirection;
		}
		return false;
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
		updateConnections(level, pos);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
			boolean isMoving) {
		if (!level.isClientSide) {
			updateConnections(level, pos);
		}
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
		  return ViaductBlockBox.getShape(
			        state,
			        CONNECTED_NORTH,
			        CONNECTED_SOUTH,
			        CONNECTED_EAST,
			        CONNECTED_WEST,
			        CONNECTED_UP,
			        CONNECTED_DOWN
			    );
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