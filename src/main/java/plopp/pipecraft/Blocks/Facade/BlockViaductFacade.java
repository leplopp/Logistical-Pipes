package plopp.pipecraft.Blocks.Facade;

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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.model.ViaductBlockBox;

public class BlockViaductFacade extends Block implements EntityBlock {

	public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
	public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
	public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");
	public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");
	public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
	public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");
	public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);
	public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
	public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");

	public BlockViaductFacade(Properties properties) {
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
	    public RenderShape getRenderShape(BlockState state) {
	        return RenderShape.MODEL;
	    }
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, CONNECTED_UP, CONNECTED_DOWN,
				LIGHT_LEVEL, COLOR, TRANSPARENT);
	}
	  @SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState copyBlockProperties(BlockState source, BlockState target) {
	        for (Property<?> prop : target.getProperties()) {
	            if (source.hasProperty(prop)) {
	                target = target.setValue((Property) prop, source.getValue(prop));
	            }
	        }
	        return target;
	    }

	 @Override
	    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
	        BlockState state = this.defaultBlockState();
	        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());
	        if (be instanceof BlockFacadeTileEntity facade) {
	            state = copyBlockProperties(facade.getOriginalBlockState(), state);
	        }
	        return state;
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

	public void updateConnections(Level level, BlockPos pos) {
		BlockState current = level.getBlockState(pos);
		if (!(current.getBlock() instanceof BlockViaductFacade))
			return;

		boolean north = isFacade(level.getBlockState(pos.relative(Direction.NORTH)));
		boolean south = isFacade(level.getBlockState(pos.relative(Direction.SOUTH)));
		boolean east = isFacade(level.getBlockState(pos.relative(Direction.EAST)));
		boolean west = isFacade(level.getBlockState(pos.relative(Direction.WEST)));
		boolean up = isFacade(level.getBlockState(pos.relative(Direction.UP)));
		boolean down = isFacade(level.getBlockState(pos.relative(Direction.DOWN)));

		BlockState updated = current.setValue(CONNECTED_NORTH, north).setValue(CONNECTED_SOUTH, south)
				.setValue(CONNECTED_EAST, east).setValue(CONNECTED_WEST, west).setValue(CONNECTED_UP, up)
				.setValue(CONNECTED_DOWN, down);

		if (!current.equals(updated)) {
			updateAndPropagate(level, pos, current, updated);
		}
	}

	private void updateAndPropagate(Level level, BlockPos pos, BlockState current, BlockState updated) {
		level.setBlock(pos, updated, 3);
		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			BlockState neighbor = level.getBlockState(neighborPos);
			if (neighbor.getBlock() instanceof BlockViaductFacade) {
				updateConnections(level, neighborPos);
			}
		}
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
			BlockPos pos, BlockPos neighborPos) {
		boolean connect = neighborState.getBlock() instanceof BlockViaductFacade;
		return state.setValue(getPropertyForDirection(direction), connect);
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
		return state.getValue(LIGHT_LEVEL);
	}

	private boolean isFacade(BlockState state) {
		return state != null && state.getBlock() instanceof BlockViaductFacade;
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
	        if (entity instanceof Player player && ViaductTravel.isTravelActive(player)) {
	            return Shapes.empty();
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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockFacadeTileEntity(pos, state);
	}

	/*@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
	                                      InteractionHand hand, BlockHitResult hitResult) {

	    if (level.isClientSide) return ItemInteractionResult.SUCCESS;

	    // Prüfen, ob Block in Config steht
	    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
	    if (!Config.getFacadableBlocks().contains(blockId)) {
	        return ItemInteractionResult.SUCCESS;
	    }

	    // Prüfen, ob Facade gesetzt werden darf (Hitbox)
	    BlockState facadeState = BlockRegister.VIADUCTFACADE.get().defaultBlockState();
	    if (!BlockFacadeTileEntity.canApplyFacade(state, facadeState)) {
	        return ItemInteractionResult.FAIL;
	    }

	    // Setze die Facade über dem Originalblock
	    level.setBlock(pos, facadeState, 3);
	    BlockEntity be = level.getBlockEntity(pos);
	    if (be instanceof BlockFacadeTileEntity facadeTE) {
	        facadeTE.setOriginalBlockState(state);
	    }

	    if (!player.isCreative()) {
	        stack.shrink(1);
	    }

	    return ItemInteractionResult.CONSUME;
	}*/
}