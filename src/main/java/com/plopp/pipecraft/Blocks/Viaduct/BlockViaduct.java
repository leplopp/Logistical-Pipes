package com.plopp.pipecraft.Blocks.Viaduct;

import java.util.List;
import javax.annotation.Nullable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.plopp.pipecraft.obj.Triangle;
import com.plopp.pipecraft.obj.objParser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


public class BlockViaduct extends Block implements EntityBlock{

    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static List<Triangle> hitboxTriangles;

	
	public static objParser objParser = new objParser(); // Initialisierung!

	public static void loadHitboxTriangles() {
	    System.out.println("[Viaduct] Hitbox-Ladeversuch...");
	    
	    if (objParser == null) {
	        System.err.println("[Viaduct] objParser ist NULL!");
	        return;
	    }
	    

	    try {
	    	 ResourceLocation location = ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct_connectet_long.obj");
	         hitboxTriangles = com.plopp.pipecraft.obj.objParser.loadTrianglesFromObj(location);
	         if (hitboxTriangles == null) {
	             System.err.println("[Viaduct] objParser lieferte NULL zurück!");
	             hitboxTriangles = List.of(); 
	         }

	         System.out.println("[Viaduct] Geladene Dreiecke: " + hitboxTriangles.size());
	     } catch (Exception e) {
	         hitboxTriangles = List.of();
	         System.err.println("[Viaduct] Fehler beim Laden der Hitbox-Daten: " + e.getMessage());
	         e.printStackTrace();
	     }
	}
	
	public BlockViaduct(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
	}      
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	    builder.add(FACING, TRIGGERED);
	}
	
	

    public void debugRender(PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer builder = buffer.getBuffer(RenderType.lines());
        for (Triangle tri : hitboxTriangles) {
            drawDebugLine(builder, poseStack, tri.a, tri.b);
            drawDebugLine(builder, poseStack, tri.b, tri.c);
            drawDebugLine(builder, poseStack, tri.c, tri.a);
        }
    }
    
    private static Vec3 rotateAroundCenter(Vec3 vec, double degrees) {
        double radians = Math.toRadians(degrees);

        double x = vec.x - 0.5;
        double z = vec.z - 0.5;

        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double rx = x * cos - z * sin;
        double rz = x * sin + z * cos;

        return new Vec3(rx + 0.5, vec.y, rz + 0.5);
    }
    
    public static List<Triangle> getRotatedTriangles(Direction facing) {
        if (hitboxTriangles == null) return List.of();

        double angle = switch (facing) {
            case NORTH -> 90;
            case EAST -> 0;
            case SOUTH -> 90;
            case WEST -> 0;
            default -> 0;
        };

    
        return hitboxTriangles.stream()
                .map(tri -> new Triangle(
                    rotateAroundCenter(tri.a, angle),
                    rotateAroundCenter(tri.b, angle),
                    rotateAroundCenter(tri.c, angle)
                )).toList();
    }

  
    private void drawDebugLine(VertexConsumer builder, PoseStack poseStack, Vec3 from, Vec3 to) {
        builder.setColor(255, 0, 0, 255);
        builder.addVertex((float) from.x, (float) from.y, (float) from.z);
        builder.addVertex((float) to.x, (float) to.y, (float) to.z);
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        List<Triangle> rotatedTris = getRotatedTriangles(facing);

        if (rotatedTris.isEmpty()) return Shapes.empty();

        VoxelShape shape = Shapes.empty();
        for (Triangle tri : rotatedTris) {
            AABB box = tri.getBoundingBox();
            shape = Shapes.or(shape, Shapes.create(box));
        }
        return shape;

    }
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
	    return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}
	 @Nullable
	    @Override
	    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
	        return new ViaductBlockEntity(pos, state);
	    }
}


