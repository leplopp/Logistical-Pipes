package plopp.pipecraft.Blocks.Facade;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.logic.Facade.IFacadable;

public class BlockFacadeTileEntity extends BlockEntity implements IFacadable {

	private BlockState facadeBlock = Blocks.AIR.defaultBlockState();

	public BlockFacadeTileEntity(BlockPos pos, BlockState state) {
		super(BlockEntityRegister.VIADUCT_FACADE.get(), pos, state);
	}

	public void setOriginalBlockState(BlockState state) {
		this.facadeBlock = state;
		setChanged();
	}

	public BlockState getOriginalBlockState() {
		return facadeBlock;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (facadeBlock != null && facadeBlock != Blocks.AIR.defaultBlockState()) {
			tag.put("OriginalBlock", NbtUtils.writeBlockState(facadeBlock));
		}
	}

	@Override
	public void setFacade(BlockState state) {
		this.facadeBlock = state;
		setChanged(); 
	}

	@Override
	public BlockState getFacade() {
		return facadeBlock;
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("OriginalBlock", Tag.TAG_COMPOUND)) {
			Optional<HolderLookup.RegistryLookup<Block>> opt = registries.lookup(Registries.BLOCK);
			if (opt.isPresent()) {
				HolderGetter<Block> blockGetter = opt.get();
				facadeBlock = NbtUtils.readBlockState(blockGetter, tag.getCompound("OriginalBlock"));
			} else {
				facadeBlock = Blocks.AIR.defaultBlockState();
			}
		} else {
			facadeBlock = Blocks.AIR.defaultBlockState();
		}
	}
	public static boolean canApplyFacade(BlockState original, BlockState facade) {
	    VoxelShape originalShape = original.getCollisionShape(null, BlockPos.ZERO);
	    VoxelShape facadeShape = facade.getCollisionShape(null, BlockPos.ZERO);
	    return originalShape.bounds().getSize() <= facadeShape.bounds().getSize();
	}
}
