package com.plopp.pipecraft.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.GameProfile;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ViaductTravel {
	
	private static final Map<UUID, Integer> ticksPerChunkMap = new HashMap<>();
	private static final Map<UUID, Integer> tickCounters = new HashMap<>();
	public static final Map<UUID, List<BlockPos>> activeTravels = new HashMap<>();
    private static final Map<UUID, Integer> travelProgress = new HashMap<>();
    private static final Map<UUID, Double> progressMap = new HashMap<>();
    private static final Set<UUID> jumpAfterTravel = new HashSet<>();
    private static final Set<UUID> jumpTrigger = new HashSet<>();
    public static final int MAX_CHARGE = 30; //charge time
    private static final Map<UUID, Integer> chargeMap = new HashMap<>();
    private static final Map<UUID, Integer> lastSentPercent = new HashMap<>();
    public static final Map<UUID, ArmorStand> headEntities = new HashMap<>();
    private static final Map<UUID, Float> lastYawMap = new HashMap<>();
    
    public static int incrementCharge(UUID id) {
        int val = chargeMap.getOrDefault(id, 0) + 1;
        chargeMap.put(id, val);
        return val;
    }

    public static boolean isCharging(UUID id) {
        return chargeMap.containsKey(id);
    }

    public static void cancelCharge(Player player) {
        cancelCharge(player.getUUID());
    }
    public static void cancelCharge(UUID id) {
        chargeMap.remove(id);
        lastSentPercent.remove(id);
    }
    
    public static void clearCharge(UUID id) {
        chargeMap.remove(id);
        lastSentPercent.remove(id);
    }

    public static int getLastSentPercent(UUID id) {
        return lastSentPercent.getOrDefault(id, -1);
    }

    public static void setLastSentPercent(UUID id, int value) {
        lastSentPercent.put(id, value);
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
        return activeTravels.containsKey(player.getUUID());
        
    }
    
    private static float normalizeYaw(double yaw) {
        while (yaw > 180) yaw -= 360;
        while (yaw < -180) yaw += 360;
        return (float) yaw;
    }
    private static float lerpAngle(float current, float target, float t) {
        float diff = target - current;
        while (diff < -180) diff += 360;
        while (diff >= 180) diff -= 360;
        return current + diff * t;
    }
    
    public static void start(Player player, BlockPos startPos, int ticksPerChunk) {
        Level level = player.level();
        BlockPos target = findTarget(level, startPos);

        if (target != null) {
            List<BlockPos> path = findViaductPath(level, startPos, target);
            if (!path.isEmpty()) {
                UUID id = player.getUUID();
                activeTravels.put(id, path);
                travelProgress.put(id, 0);
                tickCounters.put(id, 0);
                ticksPerChunkMap.put(id, ticksPerChunk);

                if (!player.level().isClientSide()) {
                    player.setInvisible(true);
                    player.setInvulnerable(true);
                    player.setSwimming(false);
                    player.setPose(Pose.STANDING);
                    player.setShiftKeyDown(false);
                    player.noPhysics = true;
                    player.setNoGravity(true);
                                         
                    ArmorStand stand = new ArmorStand(level, player.getX(), player.getY(), player.getZ());
                    stand.setInvisible(true);
                    stand.setInvulnerable(true);
                    stand.setNoGravity(true);
                    stand.setCustomName(player.getDisplayName());
                    stand.setCustomNameVisible(false);
                    byte flags = 0;
                    flags |= ArmorStand.CLIENT_FLAG_MARKER;
                    flags |= ArmorStand.CLIENT_FLAG_NO_BASEPLATE;
                    stand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, flags);

                    ItemStack head = new ItemStack(Items.PLAYER_HEAD);

                    CompoundTag skullOwner = new CompoundTag();

                    GameProfile profile = player.getGameProfile();

                    skullOwner.putString("Id", profile.getId().toString());
                    skullOwner.putString("Name", profile.getName());

                    if (profile.getProperties().containsKey("textures")) {
                        ListTag propertiesList = new ListTag();
                        for (Property property : profile.getProperties().get("textures")) {
                            CompoundTag propTag = new CompoundTag();
                            propTag.putString("Value", property.value());
                            if (property.signature() != null) {
                                propTag.putString("Signature", property.signature());
                            }
                            propertiesList.add(propTag);
                        }
                        skullOwner.put("Properties", propertiesList);
                    }

                    CompoundTag tag = new CompoundTag();
                    tag.put("SkullOwner", skullOwner);

                    head.getTags();

                    stand.setItemSlot(EquipmentSlot.HEAD, head);

                    level.addFreshEntity(stand);
                    headEntities.put(player.getUUID(), stand);
                }
            }
        }
    }
  
    public static void tick(Player player) {
        UUID id = player.getUUID();
        if (!headEntities.containsKey(id)) return;

        ArmorStand stand = headEntities.get(id);
        if (stand == null) return;

        Vec3 pos = player.position();
        stand.moveTo(pos.x, pos.y - 0.2, pos.z, stand.getYRot(), stand.getXRot());

        if (!activeTravels.containsKey(id)) return;

        List<BlockPos> path = activeTravels.get(id);
        int currentIndex = travelProgress.getOrDefault(id, 0);
        int lastIndex = path.size() - 1;
        if (currentIndex >= lastIndex) return;

        int ticksPerChunk = ticksPerChunkMap.getOrDefault(id, 1);
        if (ticksPerChunk <= 0) return;

        int tickCounter = tickCounters.getOrDefault(id, 0) + 1;
        tickCounters.put(id, tickCounter);

        double chunkProgress = progressMap.getOrDefault(id, 0.0);
        double chunkStep = 1.0 / ticksPerChunk;
        chunkProgress += chunkStep;

        int chunkSize = 16;
        int stepsLeft = Math.min(chunkSize, lastIndex - currentIndex);

        double totalStepProgress = chunkProgress * stepsLeft;
        int subIndex = (int) totalStepProgress;
        double lerpProgress = totalStepProgress - subIndex;

        int fromIndex = Math.min(currentIndex + subIndex, lastIndex - 1);
        int toIndex = Math.min(fromIndex + 1, lastIndex);

        Vec3 from = vecFromBlockPos(path.get(fromIndex));
        Vec3 to = vecFromBlockPos(path.get(toIndex));
        Vec3 lerped = from.lerp(to, lerpProgress);

        double lookAheadDistance = 0.1; 
        double lookAheadProgress = lerpProgress + lookAheadDistance;
        int lookAheadSubIndex = subIndex;

        while (lookAheadProgress > 1.0) {
            lookAheadProgress -= 1.0;
            lookAheadSubIndex++;
        }

        int lookAheadFromIndex = Math.min(currentIndex + lookAheadSubIndex, lastIndex - 1);
        int lookAheadToIndex = Math.min(lookAheadFromIndex + 1, lastIndex);

        Vec3 lookAheadFrom = vecFromBlockPos(path.get(lookAheadFromIndex));
        Vec3 lookAheadTo = vecFromBlockPos(path.get(lookAheadToIndex));
        Vec3 lookAheadPos = lookAheadFrom.lerp(lookAheadTo, lookAheadProgress);

        Vec3 direction = lookAheadPos.subtract(lerped).normalize();

        double rawYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float targetYaw = normalizeYaw(rawYaw);

        float lastYaw = lastYawMap.getOrDefault(id, targetYaw);
        float smoothYaw = lerpAngle(lastYaw, targetYaw, 0.3f); // Sanfte Rotation

        stand.setYRot(smoothYaw);
        stand.setYHeadRot(smoothYaw);
        stand.moveTo(stand.getX(), stand.getY(), stand.getZ(), smoothYaw, stand.getXRot());

        lastYawMap.put(id, smoothYaw);

        if (stand.level() instanceof ServerLevel serverLevel) {
            serverLevel.broadcastEntityEvent(stand, (byte) 3);
        }

        double y = (toIndex == lastIndex && chunkProgress >= 1.0) ? to.y + 1.0 : lerped.y - 1.0;

        if (player instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            stop(player, false);
            return;
        } else {
            player.teleportTo(lerped.x, y, lerped.z);
        }

        if (chunkProgress >= 1.0) {
            travelProgress.put(id, currentIndex + stepsLeft);
            progressMap.put(id, 0.0);
        } else {
            progressMap.put(id, chunkProgress);
        }

        if (travelProgress.get(id) >= lastIndex) {
            BlockPos stonePos = path.get(lastIndex);
            player.teleportTo(stonePos.getX() + 0.5, stonePos.getY() + 1.0, stonePos.getZ() + 0.5);

            stop(player, true);
        }
    }
    
    public static List<BlockPos> findViaductPath(Level level, BlockPos start, BlockPos end) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<List<BlockPos>> queue = new LinkedList<>();

        queue.add(List.of(start));
        visited.add(start);

        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos last = path.get(path.size() - 1);

            if (last.equals(end)) {
                if (path.size() >= 3) {
                    return path;
                } else {
                    continue; 
                }
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = last.relative(dir);
                if (visited.contains(next)) continue;

                BlockState state = level.getBlockState(next);

                if (state.is(BlockRegister.VIADUCTCHARGERBLOCK) && !next.equals(end)) continue;

                if (state.getBlock() instanceof BlockViaduct || next.equals(end)) {
                    visited.add(next);
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        return List.of(); 
    }
    
    public static BlockPos findTarget(Level level, BlockPos from) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<List<BlockPos>> queue = new LinkedList<>();

        queue.add(List.of(from));
        visited.add(from);

        int maxDistance = 512; 
        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos last = path.get(path.size() - 1);

            if (!last.equals(from) && level.getBlockState(last).is(BlockRegister.VIADUCTCHARGERBLOCK) && path.size() >= 3) {
            	 BlockPos above = last.above();
                 BlockState aboveState = level.getBlockState(above);
                 if (!(aboveState.getBlock() instanceof BlockViaduct)) {
                return last.immutable();
            }else {
                continue;
            }
       }

            for (Direction dir : Direction.values()) {
                BlockPos next = last.relative(dir);
                if (visited.contains(next)) continue;

                if (from.distManhattan(next) > maxDistance) continue; 

                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof BlockViaduct || state.is(BlockRegister.VIADUCTCHARGERBLOCK)) {
                    visited.add(next);
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        return null;
    }
    
    public static void stop(Player player, boolean includeTeleport) {
        UUID id = player.getUUID();
        ArmorStand stand = headEntities.remove(id);
        if (stand != null) {
            stand.remove(Entity.RemovalReason.DISCARDED);
        }

        List<BlockPos> path = activeTravels.remove(id);
        travelProgress.remove(id);
        tickCounters.remove(id);

        player.noPhysics = false;
        player.setPose(Pose.STANDING);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        markJumpAfterTravel(player);

        if (includeTeleport && path != null && !path.isEmpty()) {
            BlockPos stonePos = path.get(path.size() - 1);
            player.teleportTo(stonePos.getX() + 0.5, stonePos.getY() + 1.0, stonePos.getZ() + 0.5);
            BlockPos below = stonePos.below();                   // Block darunter
            BlockState belowState = player.level().getBlockState(below);

            if (belowState.getBlock() instanceof BlockViaduct) {
                System.out.println("===> SPRINGE! Viaduct unter Stone bei " + below);

                if (player.level().isClientSide() && player instanceof LocalPlayer) {
                    ((LocalPlayer) player).jumpFromGround();
                } else {
                    jumpTrigger.add(player.getUUID());
                }
             }
          }
      }
}