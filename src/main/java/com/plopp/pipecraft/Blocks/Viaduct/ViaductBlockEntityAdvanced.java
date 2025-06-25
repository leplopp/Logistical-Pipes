package com.plopp.pipecraft.Blocks.Viaduct;

import com.plopp.pipecraft.Blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ViaductBlockEntityAdvanced extends BlockEntity {
    public ViaductBlockEntityAdvanced(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIADUCTADVANCED.get(), pos, state);
    }

}