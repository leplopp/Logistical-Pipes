package com.plopp.pipecraft.Blocks.Pipes.Viaduct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Network.LinkedTargetEntry;
import com.plopp.pipecraft.logic.ViaductLinkerManager;
import com.plopp.pipecraft.logic.ViaductTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockViaductLinker extends Block implements EntityBlock {

    public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");
    public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");
    public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    public BlockViaductLinker(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CONNECTED_NORTH, false)
                .setValue(CONNECTED_SOUTH, false)
                .setValue(CONNECTED_EAST, false)
                .setValue(CONNECTED_WEST, false)
                .setValue(CORNER, false)
        		.setValue(CONNECTED_UP, false)
        		.setValue(CONNECTED_DOWN, false)
        		.setValue(FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    	builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, CONNECTED_UP, CONNECTED_DOWN, CORNER, FACING);
    }
   
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getNearestLookingDirection().getOpposite();

        BlockPos targetPos = pos.relative(facing);
        BlockState neighborState = level.getBlockState(targetPos);

        if (neighborState.getBlock() instanceof BlockViaduct || neighborState.getBlock() instanceof BlockViaductLinker) {
            return null;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighbor = level.getBlockState(neighborPos);

            if (neighbor.getBlock() instanceof BlockViaductLinker &&
                neighbor.hasProperty(FACING) &&
                neighbor.getValue(FACING) == dir.getOpposite()) {
                return null;
            }
        }

        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
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
            level.getServer().execute(() -> updateAllLinkers(level));
        }
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && oldState.getBlock() != newState.getBlock()) {
            if (level.getServer() != null) {
                level.getServer().execute(() -> updateAllLinkers(level));
                ViaductLinkerManager.removeLinker(pos);
            }
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }
    private void updateAllLinkers(Level level) {
        if (level == null || level.isClientSide) return;

        for (BlockPos pos : ViaductLinkerManager.getAllLinkers()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlockEntityViaductLinker linker) {
                List<LinkedTargetEntry> newLinkedTargets = linker.findLinkedTargetsThroughViaducts();

                if (!linker.getLinkedTargets().equals(newLinkedTargets)) {
                    linker.getLinkedTargets().clear();
                    linker.getLinkedTargets().addAll(newLinkedTargets);
                    linker.setChanged();

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    }
                }
            }
        }
    }
    
    public static void findAllConnectedLinkers(Level level, BlockPos start, Set<BlockPos> found) {
        if (found.contains(start)) return;
        found.add(start);

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = start.relative(dir);
            BlockState state = level.getBlockState(neighbor);
            BlockEntity be = level.getBlockEntity(neighbor);

            if (be instanceof BlockEntityViaductLinker) {
                findAllConnectedLinkers(level, neighbor, found);
            } else if (state.getBlock() == BlockRegister.VIADUCT.get()) {
                findAllConnectedLinkers(level, neighbor, found);
            }
        }
    }


    public void updateConnections(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof BlockViaductLinker)) return;

        boolean north = isViaduct(level.getBlockState(pos.relative(Direction.NORTH)), level, pos.relative(Direction.NORTH), Direction.SOUTH);
        boolean south = isViaduct(level.getBlockState(pos.relative(Direction.SOUTH)), level, pos.relative(Direction.SOUTH), Direction.NORTH);
        boolean east  = isViaduct(level.getBlockState(pos.relative(Direction.EAST)), level, pos.relative(Direction.EAST), Direction.WEST);
        boolean west  = isViaduct(level.getBlockState(pos.relative(Direction.WEST)), level, pos.relative(Direction.WEST), Direction.EAST);
        boolean up    = isViaduct(level.getBlockState(pos.relative(Direction.UP)), level, pos.relative(Direction.UP), Direction.DOWN);
        boolean down  = isViaduct(level.getBlockState(pos.relative(Direction.DOWN)), level, pos.relative(Direction.DOWN), Direction.UP);

        int count = 0;
        if (north) count++;
        if (south) count++;
        if (east) count++;
        if (west) count++;

        boolean isCorner = (count == 2) &&
        	    ((north && east) || (east && south) || (south && west) || (west && north));

        BlockState updated = current
        	    .setValue(CONNECTED_NORTH, north)
        	    .setValue(CONNECTED_SOUTH, south)
        	    .setValue(CONNECTED_EAST, east)
        	    .setValue(CONNECTED_WEST, west)
        	    .setValue(CONNECTED_UP, up)
        	    .setValue(CONNECTED_DOWN, down)
        	    .setValue(CORNER, isCorner)
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
        if (state == null) return false;

        Block block = state.getBlock();

        if (block instanceof BlockViaduct) return true;

    for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adjacentPos = neighborPos.relative(dir);
                BlockState adjacent = level.getBlockState(adjacentPos);

                if (adjacent.getBlock() instanceof BlockViaduct) {

                }
            }

            BlockState below = level.getBlockState(neighborPos.below());
            if (below.getBlock() instanceof BlockViaduct) {

            }

        return false;
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (!(world instanceof Level level)) return state;
        level.scheduleTick(pos, this, 1);
        return state;
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

        VoxelShape center = box(4, 4, 4, 12, 12, 12);
        VoxelShape north = box(4, 4, 0, 12, 12, 4);
        VoxelShape south = box(4, 4, 12, 12, 12, 16);
        VoxelShape west  = box(0, 4, 4, 4, 12, 12);
        VoxelShape east  = box(12, 4, 4, 16, 12, 12);
        VoxelShape up    = box(4, 12, 4, 12, 16, 12);
        VoxelShape down  = box(4, 0, 4, 12, 4, 12);
        return Shapes.or(center, north, south, west, east, up, down);
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
} 