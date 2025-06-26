package com.plopp.pipecraft.Blocks.Viaduct;

import javax.annotation.Nullable;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.plopp.pipecraft.obj.BlockViaductAdvancedShapes;
import com.plopp.pipecraft.obj.Triangle;
import com.plopp.pipecraft.obj.objParser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockViaductAdvanced extends Block {

    public static List<Triangle> hitboxTriangles;
    public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");
    public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");
    public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");
    public static objParser objParser = new objParser(); 


    public BlockViaductAdvanced(Properties properties) {
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
        return this.defaultBlockState(); // Connections werden nachträglich aktualisiert
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            updateConnections(level, pos);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
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
        if (!(current.getBlock() instanceof BlockViaductAdvanced)) return;

        boolean north = isViaductAndNotOverlapping(level, pos, Direction.NORTH);
        boolean south = isViaductAndNotOverlapping(level, pos, Direction.SOUTH);
        boolean east = isViaductAndNotOverlapping(level, pos, Direction.EAST);
        boolean west = isViaductAndNotOverlapping(level, pos, Direction.WEST);
        boolean up = isViaductAndNotOverlapping(level, pos, Direction.UP);
        boolean down = isViaductAndNotOverlapping(level, pos, Direction.DOWN);
        
        int count = 0;
        if (north) count++;
        if (south) count++;
        if (east) count++;
        if (west) count++;

        boolean isCorner = (count == 2) &&
        	    ((north && east) || (east && south) || (south && west) || (west && north));

        BlockState updated = current
                .setValue(CONNECTED_NORTH, west)
                .setValue(CONNECTED_SOUTH, east)
                .setValue(CONNECTED_EAST, north)
                .setValue(CONNECTED_WEST, south)
                .setValue(CONNECTED_UP, up)
                .setValue(CONNECTED_DOWN, down)
                .setValue(CORNER, isCorner);

        if (!current.equals(updated)) {
            level.setBlock(pos, updated, 3);
        }
    }
    
    private boolean isViaductAndNotOverlapping(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (!isViaduct(neighborState)) return false;

        // Optional: weitere Checks wie Richtung, Modellform, CustomBlockEntity-Status etc.

        return true;
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (!(world instanceof Level level)) return state;
        updateConnections(level, pos);
        return state;
    }

    private boolean isViaduct(BlockState state) {
        return state.getBlock() instanceof BlockViaductAdvanced;
    }
    private BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> CONNECTED_NORTH;
            case SOUTH -> CONNECTED_SOUTH;
            case EAST  -> CONNECTED_EAST;
            case WEST  -> CONNECTED_WEST;
            case UP    -> CONNECTED_UP;
            case DOWN  -> CONNECTED_DOWN;
        };
    }
    private String determineModelKey(BlockState state) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(getPropertyForDirection(dir))) {
                count++;
            }
        }

        boolean n = state.getValue(CONNECTED_NORTH);
        boolean s = state.getValue(CONNECTED_SOUTH);
        boolean e = state.getValue(CONNECTED_EAST);
        boolean w = state.getValue(CONNECTED_WEST);
        boolean u = state.getValue(CONNECTED_UP);
        boolean d = state.getValue(CONNECTED_DOWN);

        if (count == 0) return "default";

        if (count == 2) {
            if ((n && s) || (e && w) || (u && d)) return "long";
            return "corner";
        }

        if (count == 6) return "all_side";

        if (count == 4 && (n && s && e && w)) return "cross";

        if (count == 1 || count == 3) return "connected";

        return "connected";
    }
    
    
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        String key = determineModelKey(state);

        List<Triangle> tris = switch (key) {
            case "connected"     -> BlockViaductAdvancedShapes.CONNECTED;
            case "corner"        -> BlockViaductAdvancedShapes.CORNER;
            case "long"          -> BlockViaductAdvancedShapes.LONG;
            case "all_side"      -> BlockViaductAdvancedShapes.ALL_SIDE;
            case "cross"         -> BlockViaductAdvancedShapes.CROSS;
            case "side_rotated"  -> BlockViaductAdvancedShapes.SIDE_ROTATED;
            default              -> BlockViaductAdvancedShapes.DEFAULT;
        };

        if (tris == null || tris.isEmpty()) return Shapes.empty();

        VoxelShape shape = Shapes.empty();
        for (Triangle tri : tris) {
            shape = Shapes.or(shape, Shapes.create(tri.getBoundingBox()));
        }
        return shape;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    public void debugRender(PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer builder = buffer.getBuffer(RenderType.lines());
        for (Triangle tri : hitboxTriangles) {
            drawDebugLine(builder, poseStack, tri.a, tri.b);
            drawDebugLine(builder, poseStack, tri.b, tri.c);
            drawDebugLine(builder, poseStack, tri.c, tri.a);
        }
    }

    private void drawDebugLine(VertexConsumer builder, PoseStack poseStack, Vec3 from, Vec3 to) {
        builder.setColor(255, 0, 0, 255);
        builder.addVertex((float) from.x, (float) from.y, (float) from.z);
        builder.addVertex((float) to.x, (float) to.y, (float) to.z);
    }
} 