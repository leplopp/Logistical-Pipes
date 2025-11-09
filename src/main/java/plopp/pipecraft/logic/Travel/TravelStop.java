package plopp.pipecraft.logic.Travel;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.linker.PacketTravelJump;
import plopp.pipecraft.Network.linker.PacketTravelRotate;
import plopp.pipecraft.Network.travel.PacketTravelStop;
import plopp.pipecraft.logic.DimBlockPos;

public class TravelStop {
	
	 public static void stop(Player player, boolean includeTeleport) {
	        UUID id = player.getUUID();

	        ViaductTravel.travelYawMap.remove(id);
	        ViaductTravel.verticalDirMap.remove(id);
	        ViaductTravel.markResetModel(id);

	        TravelData data = ViaductTravel.activeTravels.remove(id);
	        if (player instanceof ServerPlayer sp) {
	            sp.getPersistentData().putBoolean("pipecraft_teleporting", false);
	        }
	        List<DimBlockPos> path = data != null ? data.path : null;

	        player.noPhysics = false;
	        player.setPose(Pose.STANDING);
	        player.setInvisible(false);
	        player.setInvulnerable(false);
	        player.setNoGravity(false);

	        if (player instanceof ServerPlayer sp) {
	            NetworkHandler.sendTravelStateToAll(sp, true);
	            NetworkHandler.sendToClient(sp, new PacketTravelStop(sp.getUUID()));
	        }

	        if (includeTeleport && path != null && !path.isEmpty()) {
	            DimBlockPos last = path.get(path.size() - 1);
	            Level level = player.level();

	            BlockPos ConnectorPos = last.getPos();
	            BlockState targetState = level.getBlockState(ConnectorPos);
	            Vec3 teleportPos = null;

	            if (targetState.is(BlockRegister.VIADUCTLINKER)) {
	                Direction facing = targetState.getValue(BlockViaductLinker.FACING);

	                double x = ConnectorPos.getX() + 0.5;
	                double z = ConnectorPos.getZ() + 0.5;
	                double y;

	                if (facing.getAxis().isHorizontal()) {
	                    double offsetAmount = 1;
	                    switch (facing) {
	                        case NORTH -> z -= offsetAmount;
	                        case SOUTH -> z += offsetAmount;
	                        case WEST  -> x -= offsetAmount;
	                        case EAST  -> x += offsetAmount;
						default -> throw new IllegalArgumentException("Unexpected value: " + facing);
	                    }

	                    BlockPos checkPos = ConnectorPos.relative(facing);
	                    BlockState checkState = level.getBlockState(checkPos);
	                    y = !checkState.isAir() ? ConnectorPos.getY() + 1.0 : ConnectorPos.getY() + 0.0;

	                } else if (facing == Direction.UP) {
	                    BlockPos checkPos = ConnectorPos.above();
	                    BlockState checkState = level.getBlockState(checkPos);
	                    y = !checkState.isAir() ? ConnectorPos.getY() + 2 : ConnectorPos.getY() + 1;

	                } else if (facing == Direction.DOWN) {
	                    BlockPos checkPos = ConnectorPos.below();
	                    BlockState checkState = level.getBlockState(checkPos);
	                    y = !checkState.isAir() ? ConnectorPos.getY() - 3 : ConnectorPos.getY() - 1;

	                } else {
	                    y = ConnectorPos.getY() + 1.0;
	                }

	                teleportPos = new Vec3(x, y, z);

	                float yaw, pitch;
	                switch (facing) {
	                    case NORTH -> yaw = 180f;
	                    case SOUTH -> yaw = 0f;
	                    case WEST  -> yaw = 90f;
	                    case EAST  -> yaw = 270f;
	                    default -> yaw = player.getYRot();
	                }

	                if (facing == Direction.UP) pitch = -90f;
	                else if (facing == Direction.DOWN) pitch = 90f;
	                else pitch = 0f;

	                player.setYRot(yaw);
	                player.setYHeadRot(yaw);
	                player.setXRot(pitch);


	                if (player instanceof ServerPlayer sp) {
	                    NetworkHandler.sendToClient(sp, new PacketTravelRotate(sp.getUUID(), yaw, pitch));
	                }

	            } else {
	                teleportPos = ViaductTravel.vecFromBlockPos(ConnectorPos);
	            }

	            if (teleportPos != null) {
	                player.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);

	                if (targetState.is(BlockRegister.VIADUCTLINKER)) {
	                    Direction facing = targetState.getValue(BlockViaductLinker.FACING);
	                    if (facing == Direction.UP && player instanceof ServerPlayer sp2) {
	                        NetworkHandler.sendToClient(sp2, new PacketTravelJump(sp2.getUUID()));
	                    }
	                }
	            }
	        }

	        if (player instanceof ServerPlayer serverPlayer) {
	            NetworkHandler.sendTravelStateToAll(serverPlayer, true);
	        }
	    }
	 
	   public static void pauseAndHold(Player player) {
	        UUID id = player.getUUID();
	        TravelData data = ViaductTravel.activeTravels.get(id);

	        if (data == null) {
	            System.out.println("[TravelStop] Kein aktiver Travel gefunden für " + player.getName().getString());
	            return;
	        }

	        data.isPaused = true;

	        player.setDeltaMovement(Vec3.ZERO);
	        player.noPhysics = true; // verhindert Kollision während Pause
	        player.setNoGravity(true);

	        System.out.println("[TravelStop] Fahrt pausiert, Spieler bleibt in Position.");
	    }
	   
	   public static void resume(Player player) {
		    UUID id = player.getUUID();
		    TravelData data = ViaductTravel.activeTravels.get(id);

		    if (data != null) {
		        data.isPaused = false;
		        System.out.println("[TravelStop] Fahrt fortgesetzt für " + player.getName().getString());
		    }
		}
}