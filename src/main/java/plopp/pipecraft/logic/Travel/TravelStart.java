package plopp.pipecraft.logic.Travel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.Blocks.ViaductBlockRegistry;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import plopp.pipecraft.logic.DimBlockPos;
import plopp.pipecraft.logic.Travel.ViaductTravel.VerticalDirection;

public class TravelStart {
	
	 private static float getYawFromDirection(Direction dir) {
		    return switch (dir) {
		        case NORTH -> 180f;
		        case SOUTH -> 0f;
		        case WEST  -> 90f;
		        case EAST  -> 270f;
		        default    -> 0f;
		    };
		}

		private static float getPitchFromDirection(Direction dir) {
		    return switch (dir) {
		        case UP   -> 90f;
		        case DOWN -> 270f;
		        default   -> 0f;
		    };
		}

		private static VerticalDirection getVerticalDirection(Direction dir) {
		    return switch (dir) {
		        case UP   -> VerticalDirection.UP;
		        case DOWN -> VerticalDirection.DOWN;
		        default   -> VerticalDirection.NONE;
		    };
		}
		
	private static void setupPlayer(Player player, BlockPos startPos) {
	    Level level = player.level();

	    if (!level.isClientSide()) {
	        player.setInvulnerable(true);
	        player.setPose(Pose.SWIMMING);
	    }

	    for (Direction dir : Direction.values()) {
	        BlockPos candidate = startPos.relative(dir);
	        BlockState state = level.getBlockState(candidate);
	        if (ViaductBlockRegistry.isViaduct(state)) {
	            Vec3 start = ViaductTravel.vecFromBlockPos(candidate);
	            player.teleportTo(start.x, start.y - 1, start.z);

	            float yaw = getYawFromDirection(dir);
	            float pitch = getPitchFromDirection(dir);

	            player.setYRot(yaw);
	            player.setXRot(pitch);

	            ViaductTravel.travelYawMap.put(player.getUUID(), yaw);
	            ViaductTravel.travelPitchMap.put(player.getUUID(), pitch);
	            ViaductTravel.verticalDirMap.put(player.getUUID(), getVerticalDirection(dir));
	            return;
	        }
	    }

	    Vec3 fallback = new Vec3(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
	    player.teleportTo(fallback.x, fallback.y, fallback.z);
	}
	
	public static void start(Player player, BlockPos startPos, BlockPos targetPos, int ticksPerChunk) {
	    Level level = player.level();
	    MinecraftServer server = player.getServer();
	    UUID uuid = player.getUUID();

	    TravelData data = new TravelData(level, startPos, targetPos, ticksPerChunk);

	    BlockEntity startBE = level.getBlockEntity(startPos);
	    if (!(startBE instanceof BlockEntityViaductLinker linker)) {
	        System.out.println("[TravelStart] Kein ViaductLinker an StartPos!");
	        TravelStop.stop(player, true);
	        return;
	    }

	    // Ziel-DimBlockPos aus den gescannten Pfaden ermitteln
	    DimBlockPos targetDimPos = linker.scannedPaths.keySet().stream()
	            .filter(d -> d.pos.equals(targetPos))
	            .findFirst()
	            .orElse(null);

	    if (targetDimPos == null) {
	        System.out.println("[TravelStart] Ziel nicht in scannedPaths gefunden!");
	        TravelStop.stop(player, true);
	        return;
	    }

	    List<DimBlockPos> fullPath = linker.scannedPaths.get(targetDimPos);
	    if (fullPath == null || fullPath.isEmpty()) {
	        System.out.println("[TravelStart] Pfad für Ziel ist leer!");
	        TravelStop.stop(player, true);
	        return;
	    }

	    boolean hasTeleporter = linker.pathsWithTeleporters.getOrDefault(targetDimPos, false);

	    if (hasTeleporter) {
	        setupPlayer(player, startPos);
	        System.out.println("[TravelStart] PathSplitter wird aufgerufen...");
	        data.pathPhase = TravelPathSplitter.splitPath(fullPath, server, targetDimPos);

	        for (int i = 0; i < data.pathPhase.size(); i++) {
	            System.out.println("[TravelStart] Phase " + i + " Länge: " + data.pathPhase.get(i).size());
	        }
	    } else {
	        System.out.println("[TravelStart] Kein Teleporter → komplette Phase verwenden");
	        data.pathPhase = new ArrayList<>();
	        data.pathPhase.add(new ArrayList<>(fullPath));
	    }

	    data.currentPhase = 0;
	    data.path = new ArrayList<>(data.pathPhase.get(0));
	    data.ticksPerChunk = data.defaultTicksPerChunk;
	    data.isPaused = false;

	    // finalTargetDimPos schon im Konstruktor gesetzt
	    data.finalTargetDimPos = targetDimPos; // <-- sichergestellt, dass es auf das richtige DimBlockPos zeigt

	    ViaductTravel.activeTravels.put(uuid, data);
	    setupPlayer(player, startPos);

	    System.out.println("[TravelStart] TravelData für Spieler " + player.getName().getString() +
	            " initialisiert. Ziel: " + targetPos);
	}
}