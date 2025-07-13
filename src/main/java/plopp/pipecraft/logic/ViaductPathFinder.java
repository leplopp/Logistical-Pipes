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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;

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
	            if (state.getBlock() instanceof BlockViaduct) {
	                return neighbor;
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

	            // Früher Abbruch, wenn ein Ziel-Linker (ViaductLinker) gefunden wurde
	            if (isViaductTarget(current)) {
	                List<BlockPos> path = new ArrayList<>();
	                BlockPos step = current;
	                while (step != null) {
	                    path.add(step);
	                    step = cameFrom.get(step);
	                }
	                Collections.reverse(path);
	                if (path.size() >= 3) {
	                    result = path;
	                }
	                pathComplete = true;
	                return;
	            }

	            // Original-Abbruch, falls das exakte Endziel erreicht wurde
	            if (current.equals(end)) {
	                List<BlockPos> path = new ArrayList<>();
	                BlockPos step = current;
	                while (step != null) {
	                    path.add(step);
	                    step = cameFrom.get(step);
	                }
	                Collections.reverse(path);
	                if (path.size() >= 3) {
	                    result = path;
	                }
	                pathComplete = true;
	                return;
	            }

	            for (Direction dir : Direction.values()) {
	                BlockPos neighbor = current.relative(dir);
	                if (!visited.contains(neighbor)) {
	                    BlockState state = level.getBlockState(neighbor);
	                    if ((state.getBlock() instanceof BlockViaduct || neighbor.equals(end))
	                            && !(state.is(BlockRegister.VIADUCTLINKER) && !neighbor.equals(end))) {
	                        visited.add(neighbor);
	                        cameFrom.put(neighbor, current);
	                        queue.add(neighbor);
	                    }
	                }
	            }
	        }
	    }

	    private boolean isViaductTarget(BlockPos pos) {
	        BlockState state = level.getBlockState(pos);
	        if (state.is(BlockRegister.VIADUCTLINKER)) {
	            BlockState above = level.getBlockState(pos.above());
	            BlockState below = level.getBlockState(pos.below());
	            // Ziel ist ein Linker, auf dem kein Viaduct-Block liegt (weder oben noch unten)
	            return !(above.getBlock() instanceof BlockViaduct) && !(below.getBlock() instanceof BlockViaduct);
	        }
	        return false;
	    }
}