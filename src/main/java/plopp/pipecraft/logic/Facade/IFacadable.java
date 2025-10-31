package plopp.pipecraft.logic.Facade;

import net.minecraft.world.level.block.state.BlockState;

public interface IFacadable {
    void setFacade(BlockState state);
    BlockState getFacade();
}