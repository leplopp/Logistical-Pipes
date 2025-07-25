package plopp.pipecraft.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.ViaductBlockRegistry;

public class ViaductPathFinder {
	
	 	private final Level level;
	 	public final BlockPos end;
	    public final Set<BlockPos> visited = new HashSet<>();
	    private final Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
	    private final Queue<BlockPos> queue = new ArrayDeque<>();
	    private boolean pathComplete = false;
	    private List<BlockPos> result = List.of();

	    public ViaductPathFinder(Level level, BlockPos start, BlockPos end) {
	        this.level = level;
	        this.end = end;

	        BlockPos startViaduct = findConnectedViaduct(start);
	        if (startViaduct == null) {
	            pathComplete = true;
	            result = List.of();
	            return;
	        }

	        queue.add(startViaduct);
	        visited.add(startViaduct);
	    }
	    
	    @Nullable
	    private BlockPos findConnectedViaduct(BlockPos linkerPos) {
	        for (Direction dir : Direction.values()) {
	            BlockPos neighbor = linkerPos.relative(dir);
	            BlockState state = level.getBlockState(neighbor);
	            if (ViaductBlockRegistry.isViaduct(state) || state.is(BlockRegister.VIADUCTLINKER)) {
	                if (ViaductBlockRegistry.areViaductBlocksConnected(level, linkerPos, neighbor)) {
	                    return neighbor;
	                }
	            }
	        }
	        return null;
	    }

	    public boolean isComplete() {
	        return pathComplete || queue.isEmpty();
	    }

	    public List<BlockPos> getResult() {
	        return result;
	    }

	    public void tick(int maxSteps) {
	        for (int i = 0; i < maxSteps && !queue.isEmpty(); i++) {
	            BlockPos current = queue.poll();

	            if (isViaductTarget(current)) {
	                result = buildPath(current);
	                pathComplete = true;
	                return;
	            }

	            if (current.equals(end)) {
	                result = buildPath(current);
	                pathComplete = true;
	                return;
	            }

	            for (Direction dir : Direction.values()) {
	                BlockPos neighbor = current.relative(dir);
	                if (visited.contains(neighbor)) continue;

	                BlockState neighborState = level.getBlockState(neighbor);
	                BlockState currentState = level.getBlockState(current);

	                boolean currentIsLinker = currentState.is(BlockRegister.VIADUCTLINKER);
	                boolean neighborIsLinker = neighborState.is(BlockRegister.VIADUCTLINKER);

	                // Linker dürfen nicht direkt verbunden sein
	                if (currentIsLinker && neighborIsLinker) continue;

	                // Verbindung erlaubt?
	                if (ViaductBlockRegistry.areViaductBlocksConnected(level, current, neighbor)) {
	                    visited.add(neighbor);
	                    cameFrom.put(neighbor, current);
	                    queue.add(neighbor);
	                }
	            }
	        }
	    }

	    private List<BlockPos> buildPath(BlockPos end) {
	        List<BlockPos> path = new ArrayList<>();
	        BlockPos step = end;
	        while (step != null) {
	            path.add(step);
	            step = cameFrom.get(step);
	        }
	        Collections.reverse(path);
	        return path;
	    }

	    private boolean isViaductTarget(BlockPos pos) {
	        return level.getBlockState(pos).is(BlockRegister.VIADUCTLINKER) && pos.equals(end);
	    }
	}