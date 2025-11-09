package plopp.pipecraft.logic.Travel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import plopp.pipecraft.logic.AsyncViaductScanner;
import plopp.pipecraft.logic.DimBlockPos;

public class TravelData {
    public final int defaultTicksPerChunk; 
    public List<DimBlockPos> path = new ArrayList<>();
    public List<List<DimBlockPos>> pathPhase = new ArrayList<>();
    public int currentPhase = 0;
	public int progressIndex = 0;
	public double chunkProgress = 0.0;
	public int ticksPerChunk;
	public int tickCounter = 0;
	public BlockPos startPos;
	public BlockPos targetPos;
	public double lockedX;
	public double lockedY;
	public double lockedZ;
	public DimBlockPos finalTargetDimPos;
	public BlockPos finalTargetPos;
    public List<DimBlockPos> nextPath = null;
	public AsyncViaductScanner scanner;
	public Map<DimBlockPos, DimBlockPos> cameFrom = new HashMap<>();
	public boolean isPaused = false;
	public boolean phaseSwitching = false;
    public DimBlockPos startDimPos;
	public DimBlockPos startTeleporterDimPos; 
	public boolean allowTeleportWhilePaused = false;
    public final Set<DimBlockPos> triggeredTeleporters = new HashSet<>();
    
	  public TravelData(Level level, BlockPos start, BlockPos end, int ticksPerChunk) {
	        this.ticksPerChunk = ticksPerChunk;
	        this.defaultTicksPerChunk = ticksPerChunk;
	        this.startPos = start;
	        this.targetPos = end;
	        this.startDimPos = new DimBlockPos(level.dimension(), start);
	        this.finalTargetPos = end;
	        this.finalTargetDimPos = new DimBlockPos(level.dimension(), end);
	        this.path.add(this.startDimPos);
	  }
	        public boolean isLastPhase() {
	            return currentPhase >= pathPhase.size() - 1;
	        }
	        
	        public void startNextPhase() {
	            currentPhase++;
	            phaseSwitching = true;
	            if (!isLastPhase()) {
	                path = new ArrayList<>(pathPhase.get(currentPhase));
	                progressIndex = 0;
	                chunkProgress = 0.0;
	            }
	        }
}