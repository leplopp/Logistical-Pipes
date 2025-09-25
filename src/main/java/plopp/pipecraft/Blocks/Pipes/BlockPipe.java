package plopp.pipecraft.Blocks.Pipes;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.logic.pipe.PipeTravel;

public class BlockPipe extends Block {
    public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty CONNECTED_EAST  = BooleanProperty.create("connected_east");
    public static final BooleanProperty CONNECTED_WEST  = BooleanProperty.create("connected_west");
    public static final BooleanProperty CONNECTED_UP    = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN  = BooleanProperty.create("connected_down");

    public BlockPipe(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(CONNECTED_NORTH, false)
            .setValue(CONNECTED_SOUTH, false)
            .setValue(CONNECTED_EAST, false)
            .setValue(CONNECTED_WEST, false)
            .setValue(CONNECTED_UP, false)
            .setValue(CONNECTED_DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, CONNECTED_UP, CONNECTED_DOWN);
    }

    public BooleanProperty getProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> CONNECTED_NORTH;
            case SOUTH -> CONNECTED_SOUTH;
            case EAST  -> CONNECTED_EAST;
            case WEST  -> CONNECTED_WEST;
            case UP    -> CONNECTED_UP;
            case DOWN  -> CONNECTED_DOWN;
        };
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            updateConnections(level, pos);
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) updateConnections(level, pos);
    }

    private void updateConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState newState = state;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            boolean connect = neighborState.getBlock() instanceof BlockPipe || canConnectToInventory(level, neighborPos, dir.getOpposite());
            newState = newState.setValue(getProperty(dir), connect);
        }

        if (newState != state) level.setBlock(pos, newState, 3);
    }

    private boolean canConnectToInventory(LevelAccessor levelAccessor, BlockPos neighborPos, Direction sideToNeighbor) {
        if (!(levelAccessor instanceof Level level)) return false;
        BlockEntity be = level.getBlockEntity(neighborPos);
        return be instanceof Container || be instanceof WorldlyContainer;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connect = neighbor.getBlock() instanceof BlockPipe || canConnectToInventory(level, neighborPos, dir.getOpposite());
        return state.setValue(getProperty(dir), connect);
    }
    
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);

    private static final VoxelShape ARM_NORTH = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape ARM_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_WEST  = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape ARM_EAST  = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_UP    = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape ARM_DOWN  = Block.box(5, 0, 5, 11, 5, 11);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CORE;

        if (state.getValue(CONNECTED_NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(CONNECTED_SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(CONNECTED_EAST))  shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(CONNECTED_WEST))  shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(CONNECTED_UP))    shape = Shapes.or(shape, ARM_UP);
        if (state.getValue(CONNECTED_DOWN))  shape = Shapes.or(shape, ARM_DOWN);

        return shape;
    }
}