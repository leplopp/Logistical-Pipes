package plopp.pipecraft.logic.pipe;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.Blocks.Pipes.BlockPipe;
import plopp.pipecraft.Blocks.Pipes.BlockPipeExtract;

public class TravellingItem {
    public final ItemStack stack;
    public final BlockPos startContainer;
    public BlockPos lastPos;
    public BlockPos currentPos;
    public BlockPos targetPos; 
    public Direction side;
    public double progress = 0.0;
    public double speed = 0.05;
    private int segmentIndex = 0;
    private double[] segmentOffsets;
    public final ServerLevel level; // hinzufügen
    public boolean pendingContainer = false;
    public double containerProgress = 0.0;
    public BlockPos containerPos = null;
    public boolean fromContainer; // neu
    public int lastExtractorIndex = -1;
    public static final Map<BlockPos, Integer> containerExtractorIndex = new HashMap<>();
    public static Map<BlockPos, Integer> pipeDirectionIndex = new HashMap<>();
    public boolean justExtracted = false;
    
    public TravellingItem(ItemStack stack, BlockPos startContainer, Direction side, PipeConfig config, ServerLevel level) {
        this.stack = stack.copy();
        this.startContainer = startContainer;
        this.lastPos = startContainer;
        this.currentPos = startContainer; // Item startet IM Container
        this.side = side;
        this.segmentOffsets = config.defaultSegmentOffsets;
        this.progress = segmentOffsets[0];
        this.segmentIndex = 0;
        this.level = level; // speichern
    }

    public void tick(Level level) {
        progress += speed;

        if (segmentIndex < segmentOffsets.length - 1 && progress >= segmentOffsets[segmentIndex + 1]) {
            segmentIndex++;

            if (segmentIndex >= segmentOffsets.length - 1) {
                Target nextTarget = PipeTravel.getNextTarget(this, currentPos, lastPos, side);

                if (nextTarget != null) {
                    lastPos = currentPos;
                    currentPos = nextTarget.pos;
                    side = nextTarget.side;
                    segmentIndex = 0;
                    progress = 0;

                    if (nextTarget.isInContainer) {
                        PipeTravel.finishItem(this, this.level);
         
                        return;
                    }
                } else {
                    PipeTravel.spawnItemEntity(this.level, currentPos, stack);
                    PipeTravel.activeItems.remove(this);
                    return;
                }
            }
        }
    }

    public Vec3 getInterpolatedPos() {
        Vec3 from = Vec3.atCenterOf(lastPos);
        Vec3 to = Vec3.atCenterOf(currentPos);
        double t = Math.max(0, Math.min(1, (progress - segmentOffsets[segmentIndex]) /
                (segmentOffsets[segmentIndex + 1] - segmentOffsets[segmentIndex])));
        return from.lerp(to, t);
    }
}