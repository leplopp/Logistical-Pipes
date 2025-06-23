package com.plopp.pipecraft.Blocks.Viaduct;

import com.plopp.pipecraft.Blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ViaductBlockEntity extends BlockEntity {
    public ViaductBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIADUCT.get(), pos, state);
    }

}