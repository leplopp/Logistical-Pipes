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
import plopp.pipecraft.Blocks.BlockEntityRegister;

public class BlockFacadeTileEntity extends BlockEntity {

	private BlockState originalBlock = Blocks.AIR.defaultBlockState();

	public BlockFacadeTileEntity(BlockPos pos, BlockState state) {
		super(BlockEntityRegister.VIADUCT_FACADE.get(), pos, state);
	}

	public void setOriginalBlockState(BlockState state) {
		this.originalBlock = state;
		setChanged();
	}

	public BlockState getOriginalBlockState() {
		return originalBlock;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);

		if (originalBlock != null && originalBlock != Blocks.AIR.defaultBlockState()) {
			tag.put("OriginalBlock", NbtUtils.writeBlockState(originalBlock));
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);

		if (tag.contains("OriginalBlock", Tag.TAG_COMPOUND)) {
			Optional<HolderLookup.RegistryLookup<Block>> opt = registries.lookup(Registries.BLOCK);
			if (opt.isPresent()) {
				HolderGetter<Block> blockGetter = opt.get();
				originalBlock = NbtUtils.readBlockState(blockGetter, tag.getCompound("OriginalBlock"));
			} else {
				originalBlock = Blocks.AIR.defaultBlockState();
			}
		} else {
			originalBlock = Blocks.AIR.defaultBlockState();
		}
	}

	public void setColor(int textColor) {
		// TODO Auto-generated method stub

	}

}
