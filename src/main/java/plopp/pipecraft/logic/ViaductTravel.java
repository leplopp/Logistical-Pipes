package plopp.pipecraft.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.ViaductBlockRegistry;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.travel.ClientTravelDataManager;
import plopp.pipecraft.Network.travel.TravelStatePacket;

public class ViaductTravel {
	
	private static final Map<UUID, Float> travelPitchMap = new HashMap<>();
	public static final Map<UUID, TravelData> activeTravels = new HashMap<>();
    private static final Set<UUID> jumpAfterTravel = new HashSet<>();
    private static final Set<UUID> jumpTrigger = new HashSet<>();
    public static final int MAX_CHARGE = 30; //charge time
    private static final Set<UUID> resetModelSet = new HashSet<>();
    private static final Map<UUID, Float> travelYawMap = new HashMap<>();
    private static final Map<UUID, VerticalDirection> verticalDirMap = new HashMap<>();
    public enum VerticalDirection {
        NONE, UP, DOWN
    }
    
    public static void markResetModel(UUID id) {
    resetModelSet.add(id);
	}

	public static boolean consumeResetModel(UUID id) {
		
    return resetModelSet.remove(id);
	}
    
    public static void markJumpAfterTravel(Player player) {
        jumpAfterTravel.add(player.getUUID());
    }

    public static boolean shouldJumpAfterTravel(Player player) {
        return jumpAfterTravel.contains(player.getUUID());
    }

    public static void clearJumpFlag(Player player) {
        jumpAfterTravel.remove(player.getUUID());
    }
    public static boolean shouldTriggerJump(UUID id) {
        return jumpTrigger.remove(id); 
    }
    
    private static Vec3 vecFromBlockPos(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }
    
    public static boolean isTravelActive(Player player) {
        if (player.level().isClientSide()) {
            TravelStatePacket travelData = ClientTravelDataManager.getTravelData(player.getUUID());
            return travelData != null && travelData.isActive();
        } else {
            return activeTravels.containsKey(player.getUUID());
        }
    }
    
    public static float getTravelYaw(UUID uuid, Level level) {
        if (level != null && level.isClientSide()) {
            TravelStatePacket travelData = ClientTravelDataManager.getTravelData(uuid);
            return travelData != null ? travelData.getTravelYaw() : 0f;
        } else {
            return travelYawMap.getOrDefault(uuid, 0f);
        }
    }
    
    public static float getTravelPitch(UUID uuid, Level level) {
        if (level != null && level.isClientSide()) {
            TravelStatePacket travelData = ClientTravelDataManager.getTravelData(uuid);
            return travelData != null ? travelData.getTravelPitch() : 0f;
        } else {
            return travelPitchMap.getOrDefault(uuid, 0f);
        }
    }
    
    public static VerticalDirection getVerticalDirection(UUID uuid, Level level) {
        if (level != null && level.isClientSide()) {
            TravelStatePacket travelData = ClientTravelDataManager.getTravelData(uuid);
            return travelData != null ? travelData.getVerticalDirection() : VerticalDirection.NONE;
        } else {
            return verticalDirMap.getOrDefault(uuid, VerticalDirection.NONE);
        }
    }
    
   public static boolean isCharging(Player player) {
        TravelData data = activeTravels.get(player.getUUID());
        return data != null && data.isCharging;
    }

   public static int getChargeProgress(Player player) {
	    if (player.level().isClientSide()) {
	        return ClientTravelDataManager.getChargeProgress(player.getUUID());
	    }

	    TravelData data = activeTravels.get(player.getUUID());
	    if (data != null) {
	        if (data.pathFinder == null || (data.pathFinder.getResult() != null && !data.pathFinder.getResult().isEmpty())) {
	            return 100;
	        }
	        return (int) data.chargeProgress;
	    }
	    return 0;
	}

    public static void start(Player player, BlockPos startPos, BlockPos targetPos, int ticksPerChunk) {
        Level level = player.level();

        TravelData data = new TravelData(level, startPos, targetPos, ticksPerChunk);

        boolean isTargetNextToStart = false;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = startPos.relative(dir);
            if (neighbor.equals(targetPos)) {
                isTargetNextToStart = true;
                break;
            }
        }

        if (isTargetNextToStart) {
            data.chargeProgress = 100;
            data.isCharging = false;
            data.path = List.of(startPos, targetPos);
            data.progressIndex = 0;
            data.pathFinder = null; 
        }
        activeTravels.put(player.getUUID(), data);

        if (!player.level().isClientSide()) {
            player.setInvulnerable(true);
            player.setShiftKeyDown(false);
            player.noPhysics = true;
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendTravelStateToAll(serverPlayer, false);
        }
        Direction travelDir = null;
        BlockPos viaduct = null;

        for (Direction dir : Direction.values()) {
            BlockPos candidate = startPos.relative(dir);
            BlockState state = level.getBlockState(candidate);
            System.out.println("Start: Check neighbor " + candidate + " block: " + state.getBlock());
            if (ViaductBlockRegistry.isViaduct(state)) {
                System.out.println("Start: Found viaduct neighbor at " + candidate);
                travelDir = dir;
                viaduct = candidate;
                break;
            }
        }

        if (viaduct != null && travelDir != null) {
            double x = viaduct.getX() + 0.5;
            double y = switch (travelDir) {
                case UP, DOWN -> viaduct.getY() - 0.5;
                default       -> viaduct.getY() - 1;
            };
            double z = viaduct.getZ() + 0.5;

            float yaw;
            float pitch;
            VerticalDirection vdir;

            if (travelDir == Direction.UP) {
                yaw = 0f;
                pitch = 90f; 
                vdir = VerticalDirection.UP;
            } else if (travelDir == Direction.DOWN) {
                yaw = 0f;
                pitch = -90f; 
                vdir = VerticalDirection.DOWN;
            } else {
                pitch = 0f;
                vdir = VerticalDirection.NONE;
                yaw = switch (travelDir) {
                    case NORTH -> 180f;
                    case SOUTH -> 0f;
                    case WEST  -> 90f;
                    case EAST  -> -90f;
                    default    -> 0f;
                };
            }

            player.teleportTo(x, y, z);
            data.lockedX = x;
            data.lockedY = y;
            data.lockedZ = z;
            player.setYRot(yaw);
            player.setXRot(pitch);

            travelYawMap.put(player.getUUID(), yaw);
            travelPitchMap.put(player.getUUID(), pitch);
            verticalDirMap.put(player.getUUID(), vdir);
        } else {
            Vec3 pos = new Vec3(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
            player.teleportTo(pos.x, pos.y, pos.z);

        }
    }
  
    public static void tick(Player player) {
        UUID id = player.getUUID();
        TravelData data = activeTravels.get(id);
        if (data == null) return;

        if (!player.level().isClientSide()) {
            List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(1.5));
            for (ItemEntity item : items) {
                item.setPickUpDelay(20);
            }
        }

        if (data.isCharging) {
            data.pathFinder.tick(50);
            List<BlockPos> result = data.pathFinder.getResult();

            if (!result.isEmpty()) {
                data.path = result;
                data.chargeProgress = 100f;
                data.isCharging = false;
                data.progressIndex = 0;
                data.chunkProgress = 0;
                data.tickCounter = 0;
                data.pathFinder = null; 
                return;
            }

            double distance = Vec3.atCenterOf(data.startPos).distanceToSqr(Vec3.atCenterOf(data.targetPos));
            double maxDistance = 512 * 512;
            double speed = 2.0 - 0.1 * Math.min(distance / maxDistance, 1.0);

            if (data.chargeProgress < 99) {
                data.chargeProgress += speed;
            }

            data.tickCounter++;

            if (!player.level().isClientSide()) {
                player.noPhysics = true;
                player.setNoGravity(true);
                player.setDeltaMovement(Vec3.ZERO);

                player.teleportTo(data.lockedX, data.lockedY, data.lockedZ);

                float yaw = ViaductTravel.getTravelYaw(player.getUUID(), player.level());
                float pitch = -ViaductTravel.getTravelPitch(player.getUUID(), player.level());

                player.setYRot(yaw);
                player.setXRot(pitch);
                player.setYHeadRot(yaw);
                player.yBodyRot = yaw;
            }

            return; 
        }

        List<BlockPos> path = data.path;
        int currentIndex = data.progressIndex;
        int lastIndex = path.size() - 1;
        if (currentIndex >= lastIndex || data.ticksPerChunk <= 0) return;

        data.tickCounter++;
        data.chunkProgress += (1.0 / data.ticksPerChunk);

        int chunkSize = 16;
        int stepsLeft = Math.min(chunkSize, lastIndex - currentIndex);
        double totalStepProgress = data.chunkProgress * stepsLeft;
        int subIndex = (int) totalStepProgress;
        double lerpProgress = totalStepProgress - subIndex;

        int fromIndex = Math.min(currentIndex + subIndex, lastIndex - 1);
        int toIndex = Math.min(fromIndex + 1, lastIndex);

        // 🟩 NEU: Schleife über alle übersprungenen Blöcke, um Speed zu erkennen
        for (int i = currentIndex; i <= fromIndex; i++) {
            BlockState state = player.level().getBlockState(path.get(i));
            if (state.getBlock() instanceof BlockViaductSpeed speedBlock) {
                int newSpeed = speedBlock.getSpeed(state); // oder state.getValue(SPEED)
                data.ticksPerChunk = newSpeed;
            }
        }
        Vec3 from = vecFromBlockPos(path.get(fromIndex));
        Vec3 to = vecFromBlockPos(path.get(toIndex));
        Vec3 lerped = from.lerp(to, lerpProgress);
        
        BlockState fromState = player.level().getBlockState(path.get(fromIndex));
        if (fromState.getBlock() instanceof BlockViaductSpeed speedBlock) {
            int newSpeed = speedBlock.getSpeed(fromState); // oder z. B. fromState.getValue(SPEED);
            data.ticksPerChunk = newSpeed;
        }
        
        double lookAheadProgress = lerpProgress + 0.1;
        int lookAheadSubIndex = subIndex;
        while (lookAheadProgress > 1.0) {
            lookAheadProgress -= 1.0;
            lookAheadSubIndex++;
        }

        int lookFromIndex = Math.min(currentIndex + lookAheadSubIndex, lastIndex - 1);
        int lookToIndex = Math.min(lookFromIndex + 1, lastIndex);
        
        
        
        Vec3 lookFrom = vecFromBlockPos(path.get(lookFromIndex));
        Vec3 lookTo = vecFromBlockPos(path.get(lookToIndex));
        Vec3 lookTarget = lookFrom.lerp(lookTo, lookAheadProgress);

        updatePlayerDirection(player, lerped, lookTarget);

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendTravelStateToAll(serverPlayer, false);
        }

        if (player instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            stop(player, false);
            return;
        }

        player.teleportTo(lerped.x, lerped.y -1, lerped.z);

        if (data.chunkProgress >= 1.0) {
            data.progressIndex += stepsLeft;
            data.chunkProgress = 0.0;
        }

        if (data.progressIndex >= lastIndex) {
            BlockPos stonePos = path.get(lastIndex);
            player.teleportTo(stonePos.getX() + 0.5, stonePos.getY() , stonePos.getZ() + 0.5);
            stop(player, true);
        }
    }
    
    public static void updatePlayerDirection(Player player, Vec3 lerped, Vec3 lookTarget) {
    	Vec3 lookDirection = lookTarget.subtract(lerped).normalize();

    	float yaw = (float) Math.toDegrees(Math.atan2(-lookDirection.x, lookDirection.z));
    	if (yaw < 0) yaw += 360.0f;
    	yaw = yaw % 360;

    	float pitch = (float) Math.toDegrees(Math.asin(lookDirection.y)); 

    	travelYawMap.put(player.getUUID(), yaw);
    	travelPitchMap.put(player.getUUID(), pitch);

    	player.setYRot(yaw);

    	double verticalThreshold = 0.8;
    	VerticalDirection dir = VerticalDirection.NONE;
    	if (lookDirection.y > verticalThreshold) {
    	    dir = VerticalDirection.UP;
    	} else if (lookDirection.y < -verticalThreshold) {
    	    dir = VerticalDirection.DOWN;
    	}
    	verticalDirMap.put(player.getUUID(), dir);
    }

    public static BlockPos findTarget(Level level, BlockPos from) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (!current.equals(from) && level.getBlockState(current).is(BlockRegister.VIADUCTLINKER)) {
                BlockPos above = current.above();
                BlockState aboveState = level.getBlockState(above);

                if (!(aboveState.getBlock() instanceof BlockViaduct)) {
                    return current.immutable();
                } else {
                    continue;
                }
            }

            BlockState currentState = level.getBlockState(current);
            boolean currentIsLinker = currentState.is(BlockRegister.VIADUCTLINKER);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);
                boolean neighborIsLinker = neighborState.is(BlockRegister.VIADUCTLINKER);

                if (neighborIsLinker && currentIsLinker) {
                    if (!ViaductBlockRegistry.isViaduct(neighborState)) {
                        continue; 
                    }
                }

                if (ViaductBlockRegistry.isViaduct(neighborState) || neighborIsLinker) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }
    
    public static void resume(Player player) {
        UUID id = player.getUUID();
        TravelData data = activeTravels.get(id);
        if (data == null) return;

        player.setInvulnerable(true);
        player.setSwimming(false);
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
    }
    
    public static void stop(Player player, boolean includeTeleport) {
        UUID id = player.getUUID();
        
        travelYawMap.remove(id);
        verticalDirMap.remove(id);
        ViaductTravel.markResetModel(player.getUUID());
        player.refreshDimensions();
        TravelData data = activeTravels.remove(id);
        List<BlockPos> path = data != null ? data.path : null;
    
        player.noPhysics = false;
        player.setPose(Pose.STANDING);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        markJumpAfterTravel(player);

        if (includeTeleport && path != null && !path.isEmpty()) {
            BlockPos stonePos = path.get(path.size() - 1);
            Level level = player.level();
            BlockState targetState = level.getBlockState(stonePos);

            Vec3 teleportPos;

            if (targetState.is(BlockRegister.VIADUCTLINKER)) {
                Direction facing = targetState.getValue(BlockViaductLinker.FACING);

                double x = stonePos.getX() + 0.5;
                double z = stonePos.getZ() + 0.5;
                double y;

                if (facing.getAxis().isHorizontal()) {

                    double offsetAmount = 1;
                    switch (facing) {
                        case NORTH -> z -= offsetAmount;
                        case SOUTH -> z += offsetAmount;
                        case WEST -> x -= offsetAmount;
                        case EAST -> x += offsetAmount;
					default -> throw new IllegalArgumentException("Unexpected value: " + facing);
                    }

                    BlockPos checkPos = stonePos.relative(facing);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (!checkState.isAir()) {
                        y = stonePos.getY() + 1.1;
                    } else {
                        y = stonePos.getY() + 0.1;
                    }
                } else if (facing == Direction.UP) {
   
                    BlockPos checkPos = stonePos.above();
                    BlockState checkState = level.getBlockState(checkPos);

                    x = stonePos.getX() + 0.5;
                    z = stonePos.getZ() + 0.5;

                    if (!checkState.isAir()) {
                        y = stonePos.getY() + 2.1; 
                    } else {
                        y = stonePos.getY() + 1.1; 
                    }
                } else if (facing == Direction.DOWN) {

                    BlockPos checkPos = stonePos.below();
                    BlockState checkState = level.getBlockState(checkPos);

                    x = stonePos.getX() + 0.5;
                    z = stonePos.getZ() + 0.5;

                    if (!checkState.isAir()) {
                        y = stonePos.getY() - 2.9;
                    } else {
                        y = stonePos.getY() - 0.9; 
                    }
                } else {
                    y = stonePos.getY() + 1.0;
                }

                teleportPos = new Vec3(x, y, z);
            } else {
                teleportPos = new Vec3(stonePos.getX() + 0.5, stonePos.getY(), stonePos.getZ() + 0.5);
            }

            player.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);

            if (targetState.is(BlockRegister.VIADUCTLINKER)) {
                Direction facing = targetState.getValue(BlockViaductLinker.FACING);

                if (facing == Direction.UP) {
                    if (player.level().isClientSide() && player instanceof LocalPlayer localPlayer) {
                        localPlayer.jumpFromGround();
                    } else {
                        jumpTrigger.add(player.getUUID());
                    }
                }
            }
        }
        
        if (player instanceof ServerPlayer serverPlayer) {
        	NetworkHandler.sendTravelStateToAll(serverPlayer, true);
            
        }
    }
    
    public static class TravelData {
        public List<BlockPos> path = new ArrayList<>();
        public int progressIndex = 0;
        public double chunkProgress = 0.0;
        public int ticksPerChunk;
        public int tickCounter = 0;
        public int maxChargeTicks = 600; 
        public float chargeProgress = 0f;
        public int chargeTickCounter = 0;
        public float ticksPerPercent = 1f; 
        public int ticksToCharge = 100;          
        public int estimatedTicksToCharge = 0; 
        public BlockPos startPos;
        public BlockPos targetPos;
        public double lockedX;
        public double lockedY;
        public double lockedZ;
    
        public boolean isCharging = true;
        public ViaductPathFinder pathFinder;

        public TravelData(Level level, BlockPos start, BlockPos end, int ticksPerChunk) {
            this.ticksPerChunk = ticksPerChunk;
            this.path.add(start);
            this.pathFinder = new ViaductPathFinder(level, start, end);
            this.startPos = start;
            this.targetPos = end;
            
        }
    }
}