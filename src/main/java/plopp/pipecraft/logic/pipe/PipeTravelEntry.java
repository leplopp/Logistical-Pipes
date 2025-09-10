package plopp.pipecraft.logic.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class PipeTravelEntry {
    public ItemStack stack;
    public BlockPos currentPos;
    public Direction movingDir;
    public float progress; // 0..100
    public boolean isInPipe = true; // Flag ob Item noch in Pipe fließt

    public PipeTravelEntry(ItemStack stack, BlockPos startPos, Direction dir) {
        this.stack = stack;
        this.currentPos = startPos;
        this.movingDir = dir;
        this.progress = 0;
    }
}