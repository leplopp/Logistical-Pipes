package plopp.pipecraft.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class TravelData {
	public DimBlockPos targetTeleporterPos = null;
    public final int defaultTicksPerChunk; 
	public List<BlockPos> path = new ArrayList<>();
	public int progressIndex = 0;
	public double chunkProgress = 0.0;
	public int ticksPerChunk;
	public int tickCounter = 0;
	public TravelPhase phase;
	public BlockPos startPos;
	public BlockPos targetPos;
	public double lockedX;
	public double lockedY;
	public double lockedZ;

	public enum TravelPhase {
		TO_START_TELEPORTER, AT_START_TELEPORTER, TO_FINAL_TARGET, FINISHED
	}

	public BlockPos finalTargetPos;
	public List<BlockPos> nextPath = null;
	public AsyncViaductScanner scanner;
	public Map<DimBlockPos, DimBlockPos> cameFrom = new HashMap<>();
	public boolean hasTeleporterPhase = true;

	public TravelData(Level level, BlockPos start, BlockPos end, int ticksPerChunk, boolean startWithScan) {
		this.ticksPerChunk = ticksPerChunk;
	    this.defaultTicksPerChunk = ticksPerChunk;
		this.path.add(start);
		this.startPos = start;
		this.targetPos = end;

		if (startWithScan) {
			this.scanner = new AsyncViaductScanner(level, start, 50, cameFrom);
		} else {
			this.scanner = null;
		}
	}
}