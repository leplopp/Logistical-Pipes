package plopp.pipecraft.Blocks.Pipes.Viaduct;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.Network.data.ViaductTeleporterWorldData;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterIdRegistry;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterManager;
import plopp.pipecraft.gui.ViaductGuiProvider;
import plopp.pipecraft.logic.Connectable;
import plopp.pipecraft.logic.ViaductTravel;

public class BlockViaductTeleporter  extends Block implements EntityBlock,Connectable{
	
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    
	   public BlockViaductTeleporter(Properties properties) {
	        super(properties);
	        this.registerDefaultState(this.defaultBlockState()
	            .setValue(FACING, Direction.NORTH));
	    }
	   
	   @Override
	   public BlockState getStateForPlacement(BlockPlaceContext context) {
	       Level level = context.getLevel();
	       BlockPos pos = context.getClickedPos();

	       for (Direction dir : Direction.values()) {
	           BlockPos neighborPos = pos.relative(dir);
	           BlockState neighborState = level.getBlockState(neighborPos);
	           Block neighborBlock = neighborState.getBlock();
	           
	           if (neighborState.getBlock() instanceof BlockViaductLinker &&
		               neighborState.hasProperty(BlockViaductLinker.FACING)) {

		               Direction neighborFacing = neighborState.getValue(BlockViaductLinker.FACING);

		               if (neighborPos.relative(neighborFacing).equals(pos)) {
		                   return null; 
		               }
		           }
	           
	           if (neighborBlock instanceof BlockViaduct || neighborBlock instanceof BlockViaductLinker) {
	               return this.defaultBlockState().setValue(FACING, dir);
	           }
	       }

	       return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	   }
	   
	   @Override
	   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
	       if (!level.isClientSide()) {

	           BlockEntity be = level.getBlockEntity(pos);
	           if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
	               sp.openMenu(provider, buf -> buf.writeBlockPos(pos));
	               return InteractionResult.CONSUME;
	           }
	       }

	       return InteractionResult.SUCCESS;
	   }
	   
	   @Override
	   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
	       super.playerWillDestroy(level, pos, state, player);

	        level.playSound(player, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
	        level.playSound(player, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
	        level.playSound(player, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
			return state;
	    }
	   
	    @Override
	    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	        builder.add(FACING);
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
	    public boolean useShapeForLightOcclusion(BlockState state) {
	        return true;
	    }
	    
	    @Override
	    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
	        if (placer instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
	            BlockEntity blockEntity = level.getBlockEntity(pos);
	            if (blockEntity instanceof BlockEntityViaductTeleporter teleporter) {
	                UUID ownerUUID = serverPlayer.getUUID();
	                teleporter.setOwnerUUID(ownerUUID); 

	                ViaductTeleporterManager.updateEntry(pos, teleporter.getStartEntry(), teleporter.getGoalEntry(), ownerUUID);
	                ViaductTeleporterWorldData.get(serverLevel).setTeleporters(ViaductTeleporterManager.getAll());
	            }
	        }
	    }
	    
	    @Override
	    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
	        if (!state.is(newState.getBlock())) {
	            BlockEntity blockEntity = level.getBlockEntity(pos);
	            if (blockEntity instanceof BlockEntityViaductTeleporter teleporter && level instanceof ServerLevel serverLevel) {
	            	String generatedId = BlockEntityViaductTeleporter.generateItemId(teleporter.getDisplayedItem());
	                if (!generatedId.isEmpty()) {
	                    ViaductTeleporterIdRegistry.unregisterId(generatedId);
	                }
	                ViaductTeleporterManager.removeEntry(serverLevel, pos);
	            }
	            super.onRemove(state, level, pos, newState, isMoving);
	            
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
	        return direction == facing; 
	    }

	    @Override
	    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
	        return new BlockEntityViaductTeleporter(pos, state);
	    }
	}
