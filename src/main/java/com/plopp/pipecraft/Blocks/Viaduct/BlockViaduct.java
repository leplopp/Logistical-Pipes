package com.plopp.pipecraft.Blocks.Viaduct;

import com.plopp.pipecraft.logic.ViaductTravel;
import com.plopp.pipecraft.obj.objParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
    public static objParser objParser = new objParser(); 

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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, CORNER, CONNECTED_UP, CONNECTED_DOWN);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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

    
    public void updateConnections(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof BlockViaduct)) return;

        if (isCrossing(level, pos)) {
            setConnections(level, pos, false, false, false, false, false, false, false);
            return;
        }

        boolean north = isViaduct(level.getBlockState(pos.relative(Direction.NORTH)), level, pos.relative(Direction.NORTH), Direction.SOUTH) && !isCrossing(level, pos.relative(Direction.NORTH));
        boolean south = isViaduct(level.getBlockState(pos.relative(Direction.SOUTH)), level, pos.relative(Direction.SOUTH), Direction.NORTH) && !isCrossing(level, pos.relative(Direction.SOUTH));
        boolean east  = isViaduct(level.getBlockState(pos.relative(Direction.EAST)), level, pos.relative(Direction.EAST), Direction.WEST) && !isCrossing(level, pos.relative(Direction.EAST));
        boolean west  = isViaduct(level.getBlockState(pos.relative(Direction.WEST)), level, pos.relative(Direction.WEST), Direction.EAST) && !isCrossing(level, pos.relative(Direction.WEST));
        boolean up    = isViaduct(level.getBlockState(pos.relative(Direction.UP)), level, pos.relative(Direction.UP), Direction.DOWN) && !isCrossing(level, pos.relative(Direction.UP));
        boolean down  = isViaduct(level.getBlockState(pos.relative(Direction.DOWN)), level, pos.relative(Direction.DOWN), Direction.UP) && !isCrossing(level, pos.relative(Direction.DOWN));

        int connectionCount = 0;
        if (north) connectionCount++;
        if (south) connectionCount++;
        if (east)  connectionCount++;
        if (west)  connectionCount++;
        if (up)    connectionCount++;
        if (down)  connectionCount++;

        if (connectionCount > 2) {
            setConnections(level, pos, false, false, false, false, false, false, false);
            return;
        }

        boolean isStraightLineNS = north && south && !east && !west && !up && !down;
        boolean isStraightLineEW = east && west && !north && !south && !up && !down;
        boolean isStraightLineUD = up && down && !north && !south && !east && !west;

        boolean isCorner = (north && east) || (east && south) ||
                           (south && west) || (west && north) ||
                           (up && north)   || (up && south) ||
                           (up && east)    || (up && west)  ||
                           (down && north) || (down && south) ||
                           (down && east)  || (down && west);

        if (isStraightLineNS && (east || west || up || down)) {
            east = west = up = down = false;
        } else if (isStraightLineEW && (north || south || up || down)) {
            north = south = up = down = false;
        } else if (isStraightLineUD && (north || south || east || west)) {
            north = south = east = west = false;
        }

        if (connectionCount == 2 && !isStraightLineNS && !isStraightLineEW && !isStraightLineUD && !isCorner) {
            setConnections(level, pos, false, false, false, false, false, false, false);
            return;
        }

        setConnections(level, pos, north, south, east, west, up, down, isCorner);
    }
    
    private void setConnections(Level level, BlockPos pos,
            boolean north, boolean south, boolean east, boolean west,
            boolean up, boolean down, boolean corner) {
    		BlockState current = level.getBlockState(pos);
    		BlockState updated = current
    						.setValue(CONNECTED_NORTH, north)
    						.setValue(CONNECTED_SOUTH, south)
    						.setValue(CONNECTED_EAST, east)
    						.setValue(CONNECTED_WEST, west)
    						.setValue(CONNECTED_UP, up)
    						.setValue(CONNECTED_DOWN, down)
    						.setValue(CORNER, corner);

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
    
    private boolean isCrossing(Level level, BlockPos pos) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (isViaduct(neighborState, level, neighborPos, dir.getOpposite())) {
                count++;
            }
        }
        return count >= 3;
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

        if (block instanceof BlockViaduct) return true;

        if ((block == Blocks.STONE ) && toDirection != Direction.UP) {
            int viaductConnections = 0;
            Direction foundDir = null;

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adjacentPos = neighborPos.relative(dir);
                BlockState adjacent = level.getBlockState(adjacentPos);

                if (adjacent.getBlock() instanceof BlockViaduct) {
                    viaductConnections++;
                    foundDir = dir;
                }
            }

            // Unten auch zulassen
            BlockState below = level.getBlockState(neighborPos.below());
            if (below.getBlock() instanceof BlockViaduct) {
                viaductConnections++;
                foundDir = Direction.DOWN;
            }

            // Nur eine Verbindung erlaubt, und zwar in unsere Richtung
            return viaductConnections == 1 && toDirection == foundDir;
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
                    return Shapes.empty(); // keine visuelle Hitbox
                }
            }
        }

        // „Stern“-Form wie vorher
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
        // Prüfe ob eine Entität mit dem Kontext verbunden ist (Spieler)
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof Player player) {
                // Wenn Spieler aktiv auf einer Viaduct-Reise ist, Hitbox entfernen
                if (ViaductTravel.isTravelActive(player)) {
                    return Shapes.empty();
                }
            }
        }
        // Sonst normale Hitbox zurückgeben
        return getShape(state, world, pos, context);
    }
} 
