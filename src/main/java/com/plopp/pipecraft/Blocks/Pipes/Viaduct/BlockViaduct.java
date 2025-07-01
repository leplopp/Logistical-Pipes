package com.plopp.pipecraft.Blocks.Pipes.Viaduct;

import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.logic.ViaductLinkerManager;
import com.plopp.pipecraft.logic.ViaductTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockViaduct extends Block {

    public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");
    public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");
    public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");

    public BlockViaduct(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CONNECTED_NORTH, false)
                .setValue(CONNECTED_SOUTH, false)
                .setValue(CONNECTED_EAST, false)
                .setValue(CONNECTED_WEST, false)
                .setValue(CORNER, false)
        		.setValue(CONNECTED_UP, false)
        		.setValue(CONNECTED_DOWN, false));
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            boolean connectsToNetwork = false;

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() == BlockRegister.VIADUCT.get()
                    || neighborState.getBlock() == BlockRegister.VIADUCTLINKER.get()) {
                    connectsToNetwork = true;
                    break;
                }
            }

            if (connectsToNetwork) {
                // Netzwerk nur dann updaten, wenn Verbindung vorhanden
                ViaductLinkerManager.updateAllLinkers(level);
            }
        }
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, CORNER, CONNECTED_UP, CONNECTED_DOWN);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof BlockViaductLinker &&
                neighborState.hasProperty(BlockViaductLinker.FACING)) {

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

            if (level.getServer() != null) {
                level.getServer().execute(() -> ViaductLinkerManager.updateAllLinkers(level));
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            // Globale Linker-Verbindungen neu berechnen
            if (level.getServer() != null) {
                level.getServer().execute(() -> ViaductLinkerManager.updateAllLinkers(level));
            }

            // Lokale Verbindungsgrafik aktualisieren (optional)
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                updateConnections(level, pos.relative(dir));
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
    public void updateConnections(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof BlockViaduct)) return;

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
                .setValue(CORNER, isCorner);

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
                if (neighborState.getBlock() instanceof BlockViaduct) {
                    updateConnections(level, neighborPos);
                }
            }
        }
    }
    
    private boolean isViaduct(BlockState state, Level level, BlockPos neighborPos, Direction toDirection) {
        if (state == null) return false;

        Block block = state.getBlock();

        return block instanceof BlockViaduct || block instanceof BlockViaductLinker;
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
} 