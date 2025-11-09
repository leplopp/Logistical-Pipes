package plopp.pipecraft.Blocks.Pipes.Viaduct;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;
import plopp.pipecraft.logic.Connectable;
import plopp.pipecraft.logic.SpeedLevel;
import plopp.pipecraft.logic.Travel.ViaductTravel;

public class BlockViaductSpeed  extends Block implements EntityBlock, Connectable{
	
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING; 
    public static final EnumProperty<SpeedLevel> SPEED = EnumProperty.create("speed", SpeedLevel.class);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");
    public static BlockPos editingPos = null;
    public static boolean editingActive = false;


    
	   public BlockViaductSpeed(Properties properties) {
	        super(properties);
	        this.registerDefaultState(this.defaultBlockState()
	            .setValue(FACING, Direction.NORTH)
                .setValue(COLOR, DyeColor.WHITE)
                .setValue(TRANSPARENT, true)
	            .setValue(SPEED, SpeedLevel.LEVEL_32));
	        
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
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
		   return false; 
	}
	   
	   @Override
	   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
	       ItemStack held = player.getMainHandItem();

	       if (held.getItem() instanceof DyeItem ||
	           held.is(ItemTags.WOOL) ||
	           held.is(BlockRegister.VIADUCT.asItem()) ||
	           held.is(Items.GLASS)) {
	           return InteractionResult.PASS;
	       }
		   
		   if (player.isShiftKeyDown()) {
	           BlockEntity be = level.getBlockEntity(pos);
	           if (be instanceof BlockEntityViaductSpeed speedBE) {
	               speedBE.setIdStack(ItemStack.EMPTY);
	               speedBE.setChanged();

	               if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
	                   ClientboundBlockEntityDataPacket pkt = speedBE.getUpdatePacket();
	                   if (pkt != null) {
	                       serverLevel.getPlayers(p -> p.blockPosition().closerThan(pos, 64))
	                                  .forEach(sp -> sp.connection.send(pkt));
	                   }
	               }
	           }

	           return InteractionResult.CONSUME;
	       }

	       if (level.isClientSide) {
	           if (BlockViaductSpeed.editingActive && BlockViaductSpeed.editingPos != null && BlockViaductSpeed.editingPos.equals(pos)) {
	               BlockViaductSpeed.editingActive = false;
	               BlockViaductSpeed.editingPos = null;

	               if (state.hasProperty(BlockViaductSpeed.SPEED)) {
	            	    int speed = state.getValue(BlockViaductSpeed.SPEED).getValue();
	                   player.displayClientMessage(
	                		   Component.translatable("viaduct.speed.change.end.correct", speed), 
	                       true
	                   );
	               } else {
	                   player.displayClientMessage(
	                		   Component.translatable("viaduct.speed.change.end"), 
	                       true
	                   );
	               }
	           } else {
	               BlockViaductSpeed.editingActive = true;
	               BlockViaductSpeed.editingPos = pos;
	               player.displayClientMessage(Component.translatable("viaduct.speed.change.start"), true);
	           }
	       }

	       return InteractionResult.SUCCESS;
	   }
	   
	   @Override
	   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
	       super.playerWillDestroy(level, pos, state, player);

	        level.playSound(player, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
	        level.playSound(player, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
			return state;
	    }

		@Override
		public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		    ItemStack stack = new ItemStack(this.asItem());

		    if (state.hasProperty(BlockViaductSpeed.COLOR)) {
		        DyedViaductItem.setColor(stack, state.getValue(BlockViaductSpeed.COLOR));
		    }
		    popResource(world, pos, stack); 
		}
		
	    @Override
	    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	        builder.add(FACING, SPEED, COLOR, TRANSPARENT);
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

	    public int getSpeed(BlockState state) {
	        return state.getValue(SPEED).getValue();
	    }

	    @Override
	    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
	        return new BlockEntityViaductSpeed(pos, state);
	    }
	}