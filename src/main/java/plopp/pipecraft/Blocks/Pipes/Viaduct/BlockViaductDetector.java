package plopp.pipecraft.Blocks.Pipes.Viaduct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.logic.Connectable;
import plopp.pipecraft.logic.ViaductTravel;

public class BlockViaductDetector  extends Block implements Connectable{
	
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    
	   public BlockViaductDetector(Properties properties) {
	        super(Properties.of()
	            .strength(1.5f));
	        this.registerDefaultState(this.defaultBlockState()
	            .setValue(FACING, Direction.NORTH)
	            .setValue(POWERED, false));
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
	       
	       Direction face = context.getClickedFace();
	       return this.defaultBlockState().setValue(FACING, face.getOpposite());
	   }
	   
	    @Override
	    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	        builder.add(FACING, POWERED);
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
	    public boolean isSignalSource(BlockState state) {
	        return true;
	    }

	    @Override
	    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
	        return state.getValue(POWERED) ? 15 : 0;
	    }

	    @Override
	    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
	        if (state.getValue(POWERED)) {
	            level.setBlock(pos, state.setValue(POWERED, false), 3);
	            level.updateNeighborsAt(pos, this);
	        }
	    }

	    @Override
	    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
	        if (!level.isClientSide && !state.getValue(POWERED)) {
	            if (entity instanceof Player player) {
	                if (ViaductTravel.isTravelActive(player)) {
	                    level.setBlock(pos, state.setValue(POWERED, true), 3);
	                    level.updateNeighborsAt(pos, this);
	                    level.scheduleTick(pos, this, 20);
	                }
	            }
	        }
	        super.entityInside(state, level, pos, entity);
	    }
	    
	    @Override
	    public boolean useShapeForLightOcclusion(BlockState state) {
	        return true;
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
	    
	        Direction facing = state.getValue(FACING);

	        VoxelShape northArm = box(4, 0, 0, 12, 16, 16);
	        VoxelShape westArm  = box(0, 4, 0, 16, 12, 16);
	        VoxelShape defaultShape = Shapes.or(northArm, westArm);

	        return rotateShape(defaultShape, facing);
	    }
	    
	    private static VoxelShape rotateShape(VoxelShape shape, Direction direction) {
	        VoxelShape[] buffer = new VoxelShape[] { shape, Shapes.empty() };

	        for (AABB box : shape.toAabbs()) {
	            AABB rotated = switch (direction) {
	                case NORTH -> new AABB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY, 1 - box.minZ);
	                case EAST  -> new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
	                case WEST  -> new AABB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
	                case UP    -> new AABB(box.minX, box.minZ, 1 - box.maxY, box.maxX, box.maxZ, 1 - box.minY);
	                case DOWN  -> new AABB(box.minX, 1 - box.maxZ, box.minY, box.maxX, 1 - box.minZ, box.maxY);
	                default    -> box;
	            };
	            buffer[1] = Shapes.or(buffer[1], Shapes.create(rotated));
	        }

	        return buffer[1];
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
	        return direction.getAxis() == facing.getAxis();
	    }
	}
