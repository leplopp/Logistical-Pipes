package plopp.pipecraft.Blocks.Pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.logic.pipe.PipeTravel;

public class BlockPipeExtract extends BlockPipe implements EntityBlock{
    private final PipeConfig config;

    public BlockPipeExtract(Properties properties, PipeConfig config) {
        super(properties);
        this.config = config;
        this.registerDefaultState(this.defaultBlockState());
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof Container container) {
                ItemStack extracted = extractItems(container, 64);
                if (!extracted.isEmpty()) {
                	PipeTravel.insertItem(extracted, neighbor, dir, level, config);
                }
            }
        }

        level.scheduleTick(pos, this, 20); 
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
    
    private ItemStack extractItems(Container container, int amount) {
        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize() && amount > 0; i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty()) {
                int take = Math.min(amount, slot.getCount());
                if (result.isEmpty()) {
                    result = slot.split(take);
                } else if (ItemStack.isSameItem(result, slot)) {
                    result.grow(take);
                    slot.shrink(take);
                }
                container.setChanged();
                amount -= take;
            }
        }
        return result;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockPipeExtractEntity(pos, state);
    }
}