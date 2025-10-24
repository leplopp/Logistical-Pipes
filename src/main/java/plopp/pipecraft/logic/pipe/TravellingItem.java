package plopp.pipecraft.logic.pipe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.pipes.TravellingItemRemovePacket;

public class TravellingItem {
    public ItemStack stack;
    public final BlockPos startContainer;
    public BlockPos lastPos;
    public BlockPos currentPos;
    public BlockPos targetPos; 
    public Direction side;
    public double progress = 0.0;
    public double speed = 0.05;
    private int segmentIndex = 0;
    private double[] segmentOffsets;
    public ServerLevel level; 
    public boolean pendingContainer = false;
    public double containerProgress = 0.0;
    public BlockPos containerPos = null;
    public boolean fromContainer; 
    public int lastExtractorIndex = -1;
    public static final Map<BlockPos, Integer> containerExtractorIndex = new HashMap<>();
    public static Map<BlockPos, Integer> pipeDirectionIndex = new HashMap<>();
    public boolean justExtracted = false;
    public UUID id = UUID.randomUUID(); 
    
    public TravellingItem(ItemStack stack, BlockPos startContainer, Direction side, PipeConfig config, ServerLevel level) {
        this.stack = stack.copy();
        this.startContainer = startContainer;
        this.lastPos = startContainer;
        this.currentPos = startContainer;
        this.side = side;
        this.segmentOffsets = config.defaultSegmentOffsets;
        this.progress = segmentOffsets[0];
        this.segmentIndex = 0;
        this.level = level; 
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
                        TravellingItemRemovePacket removePkt = new TravellingItemRemovePacket(this.id);
                        for (ServerPlayer p : this.level.players()) {
                                NetworkHandler.sendToClient(p, removePkt);
                        }
                        return;
                    }
                } else {
                	   ItemStack dropStack = this.stack.copy();

                	    PipeTravel.spawnItemEntity(this.level, currentPos, dropStack);

                	    this.stack.setCount(0); 
                	    PipeTravel.activeItems.remove(this);

                	    TravellingItemRemovePacket removePkt = new TravellingItemRemovePacket(this.id);
                	    for (ServerPlayer p : this.level.players()) {
                	        NetworkHandler.sendToClient(p, removePkt);
                	    }
                	    return;
                }
            }
        }
    }
    
    public void clientTick(Level level) {
        progress += speed;
        if (progress >= 1.0) {
            progress = 1.0;
            lastPos = currentPos;
            stack = ItemStack.EMPTY; 
        }
    }

    public Vec3 getInterpolatedPos(float partialTick) {
        Vec3 from = Vec3.atCenterOf(lastPos);
        Vec3 to = Vec3.atCenterOf(currentPos);
        double interp = Math.min(1.0, Math.max(0.0, progress + partialTick * speed));
        return from.lerp(to, interp);
    }
    
    public boolean isFinished() {
        return stack.isEmpty();
    }
}