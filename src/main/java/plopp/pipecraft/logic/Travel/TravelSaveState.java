package plopp.pipecraft.logic.Travel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.logic.DimBlockPos;
import plopp.pipecraft.logic.Travel.ViaductTravel.VerticalDirection;

public class TravelSaveState {
	 public static CompoundTag saveToTag(Player player) {
	        CompoundTag tag = new CompoundTag();
	        TravelData data = ViaductTravel.activeTravels.get(player.getUUID());
	        if (data == null) return tag;

	        tag.putInt("TicksPerChunk", data.ticksPerChunk);
	        tag.putInt("DefaultTicksPerChunk", data.defaultTicksPerChunk);
	        tag.putInt("ProgressIndex", data.progressIndex);
	        tag.putDouble("ChunkProgress", data.chunkProgress);
	        tag.putInt("TickCounter", data.tickCounter);
	        if (data.startPos != null) tag.putLong("StartPos", data.startPos.asLong());
	        if (data.targetPos != null) tag.putLong("TargetPos", data.targetPos.asLong());
	        if (data.finalTargetPos != null) tag.putLong("FinalTargetPos", data.finalTargetPos.asLong());
	        tag.putDouble("LockedX", data.lockedX);
	        tag.putDouble("LockedY", data.lockedY);
	        tag.putDouble("LockedZ", data.lockedZ);
	        tag.putInt("CurrentPhase", data.currentPhase);

	        ListTag phasesTag = new ListTag();
	        for (List<DimBlockPos> phase : data.pathPhase) {
	            ListTag phaseList = new ListTag();
	            for (DimBlockPos pos : phase) {
	                phaseList.add(pos.save());
	            }
	            phasesTag.add(phaseList);
	        }
	        tag.put("PathPhase", phasesTag);

	        ListTag pathList = new ListTag();
	        for (DimBlockPos pos : data.path) {
	            pathList.add(pos.save());
	        }
	        tag.put("Path", pathList);

	        if (data.nextPath != null) {
	            ListTag nextPathList = new ListTag();
	            for (DimBlockPos pos : data.nextPath) {
	                nextPathList.add(pos.save());
	            }
	            tag.put("NextPath", nextPathList);
	        }

	        if (!data.cameFrom.isEmpty()) {
	            ListTag cameFromList = new ListTag();
	            for (Map.Entry<DimBlockPos, DimBlockPos> entry : data.cameFrom.entrySet()) {
	                CompoundTag cf = new CompoundTag();
	                cf.put("From", entry.getKey().save());
	                cf.put("To", entry.getValue().save());
	                cameFromList.add(cf);
	            }
	            tag.put("CameFrom", cameFromList);
	        }

	        return tag;
	    }

	    public static TravelData loadFromTag(Level level, CompoundTag tag) {
	        BlockPos start = tag.contains("StartPos") ? BlockPos.of(tag.getLong("StartPos")) : BlockPos.ZERO;
	        BlockPos target = tag.contains("TargetPos") ? BlockPos.of(tag.getLong("TargetPos")) : BlockPos.ZERO;

	        int ticksPerChunk = tag.getInt("TicksPerChunk");
	        TravelData data = new TravelData(level, start, target, ticksPerChunk);

	        data.progressIndex = tag.getInt("ProgressIndex");
	        data.chunkProgress = tag.getDouble("ChunkProgress");
	        data.tickCounter = tag.getInt("TickCounter");
	        data.lockedX = tag.getDouble("LockedX");
	        data.lockedY = tag.getDouble("LockedY");
	        data.lockedZ = tag.getDouble("LockedZ");
	        data.currentPhase = tag.getInt("CurrentPhase");

	        if (tag.contains("PathPhase")) {
	            ListTag phasesTag = tag.getList("PathPhase", Tag.TAG_LIST);
	            data.pathPhase = new ArrayList<>();
	            for (Tag t : phasesTag) {
	                ListTag phaseListTag = (ListTag) t;
	                List<DimBlockPos> phase = new ArrayList<>();
	                for (Tag pt : phaseListTag) {
	                    phase.add(DimBlockPos.load((CompoundTag) pt));
	                }
	                data.pathPhase.add(phase);
	            }
	            if (!data.pathPhase.isEmpty() && data.currentPhase < data.pathPhase.size()) {
	                data.path = new ArrayList<>(data.pathPhase.get(data.currentPhase));
	            }
	        }

	        if (tag.contains("FinalTargetPos")) data.finalTargetPos = BlockPos.of(tag.getLong("FinalTargetPos"));

	        ListTag pathList = tag.getList("Path", Tag.TAG_COMPOUND);
	        for (Tag entry : pathList) {
	            data.path.add(DimBlockPos.load((CompoundTag) entry));
	        }

	        if (tag.contains("NextPath")) {
	            ListTag nextList = tag.getList("NextPath", Tag.TAG_COMPOUND);
	            List<DimBlockPos> np = new ArrayList<>();
	            for (Tag entry : nextList) {
	                np.add(DimBlockPos.load((CompoundTag) entry));
	            }
	            data.nextPath = np;
	        }

	        if (tag.contains("CameFrom")) {
	            ListTag cfList = tag.getList("CameFrom", Tag.TAG_COMPOUND);
	            for (Tag t : cfList) {
	                CompoundTag cf = (CompoundTag) t;
	                DimBlockPos from = DimBlockPos.load(cf.getCompound("From"));
	                DimBlockPos to = DimBlockPos.load(cf.getCompound("To"));
	                data.cameFrom.put(from, to);
	            }
	        }

	        return data;
	    }
	    
	    public static void resumeFromTag(ServerPlayer player, CompoundTag tag) {
	        TravelData travel = loadFromTag(player.level(), tag);
	        ViaductTravel.activeTravels.put(player.getUUID(), travel);
	        NetworkHandler.sendTravelStateToAll(player, true);
	        resume(player);
	    }
	    
	    
	    public static void resume(Player player) {
	        UUID id = player.getUUID();

	        TravelData data = ViaductTravel.getTravelData(player);

	        if (data != null) {
	            data.isPaused = false;
	        }
	        if (data == null) return;

	        player.setInvulnerable(true);
	        player.setSwimming(false);
	        player.noPhysics = true;
	        player.setNoGravity(true);
	        player.setDeltaMovement(Vec3.ZERO);
	        player.setPose(Pose.SWIMMING);

	        ViaductTravel.travelYawMap.put(id, player.getYRot());
	        ViaductTravel.travelPitchMap.put(id, player.getXRot());
	        ViaductTravel.verticalDirMap.put(id, VerticalDirection.NONE);

	        if (data.path != null && !data.path.isEmpty() && data.progressIndex < data.path.size()) {
	            Vec3 pos = ViaductTravel.vecFromDimBlockPos(data.path.get(data.progressIndex));
	            player.teleportTo(pos.x, pos.y, pos.z);
	        }

	        player.refreshDimensions();
	    }
}