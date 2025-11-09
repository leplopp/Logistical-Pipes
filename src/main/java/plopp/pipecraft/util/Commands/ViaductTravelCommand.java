package plopp.pipecraft.util.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Network.NetworkHandler;
import plopp.pipecraft.Network.linker.PacketTravelJump;
import plopp.pipecraft.Network.linker.PacketTravelRotate;
import plopp.pipecraft.logic.Travel.TravelData;
import plopp.pipecraft.logic.Travel.TravelStop;
import plopp.pipecraft.logic.Travel.ViaductTravel;

public class ViaductTravelCommand {
	
	 public static void register(RegisterCommandsEvent event) {
	        event.getDispatcher().register(
	            Commands.literal("viaduct")
	                .requires(source -> source.hasPermission(0))
	                .then(Commands.literal("travel")
	                
	                		   .then(Commands.literal("stop")
	                                   .then(Commands.literal("now")
	                                       .executes(ViaductTravelCommand::runStopNow))
	                                   .then(Commands.literal("tostart")
	                                       .executes(ViaductTravelCommand::runStopToStart))
	                               )
	                		   .then(Commands.literal("pause")
	                                   .executes(ViaductTravelCommand::runPause))
	                               .then(Commands.literal("resume")
	                                   .executes(ViaductTravelCommand::runResume))
	                           )
	                   );
	               }

	    private static int runStopNow(CommandContext<CommandSourceStack> ctx) {
	        CommandSourceStack source = ctx.getSource();

	        if (!(source.getEntity() instanceof ServerPlayer player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.stop.onlyplayer"));
	            return 0;
	        }

	        if (!ViaductTravel.isTravelActive(player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.stop.noactivetravel"));
	            return 0;
	        }

	        TravelStop.stop(player, false);
	        source.sendSuccess(() -> Component.translatable("pipecraft.commands.travel.stop.now"), false);
	        return Command.SINGLE_SUCCESS;
	    }

	    private static int runStopToStart(CommandContext<CommandSourceStack> ctx) {
	        CommandSourceStack source = ctx.getSource();

	        if (!(source.getEntity() instanceof ServerPlayer player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.stop.onlyplayer"));
	            return 0;
	        }

	        if (!ViaductTravel.isTravelActive(player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.stop.noactivetravel"));
	            return 0;
	        }

	        TravelData data = ViaductTravel.getTravelData(player);
	        if (data == null || data.startDimPos == null) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.stop.nostartpos"));
	            return 0;
	        }

	        ServerLevel startLevel = player.server.getLevel(data.startDimPos.getDimension());
	        if (startLevel == null) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.stop.nulldimension"));
	            return 0;
	        }

            BlockPos startPos = data.startDimPos.getPos();
            BlockState state = startLevel.getBlockState(startPos);
            Vec3 teleportPos;

            double x = startPos.getX() + 0.5;
            double y = startPos.getY() + 1.0;
            double z = startPos.getZ() + 0.5;

            float yaw = player.getYRot();
            float pitch = player.getXRot();

            if (state.is(BlockRegister.VIADUCTLINKER.get())) {
                Direction facing = state.getValue(BlockViaductLinker.FACING);
                double offset = 1.0;

                switch (facing) {
                    case NORTH -> z -= offset;
                    case SOUTH -> z += offset;
                    case WEST  -> x -= offset;
                    case EAST  -> x += offset;
                    default -> {}
                }

                BlockPos checkPos = startPos.relative(facing);
                BlockState checkState = startLevel.getBlockState(checkPos);
                if (!checkState.isAir()) y += 1.0;

                switch (facing) {
                    case NORTH -> yaw = 180f;
                    case SOUTH -> yaw = 0f;
                    case WEST  -> yaw = 90f;
                    case EAST  -> yaw = 270f;
                    case UP -> pitch = -90f;
                    case DOWN -> pitch = 90f;
                }
            }

            teleportPos = new Vec3(x, y, z);

            player.teleportTo(startLevel, teleportPos.x, teleportPos.y, teleportPos.z, yaw, pitch);

            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            player.setXRot(pitch);

            if (player instanceof ServerPlayer sp) {
                NetworkHandler.sendToClient(sp, new PacketTravelRotate(sp.getUUID(), yaw, pitch));
                if (state.is(BlockRegister.VIADUCTLINKER.get()) && state.getValue(BlockViaductLinker.FACING) == Direction.UP) {
                	NetworkHandler.sendToClient(sp, new PacketTravelJump(sp.getUUID()));
                }
            }

            TravelStop.stop(player, false);

            player.displayClientMessage(Component.translatable("pipecraft.commands.travel.stop.tostart"), true);
            return Command.SINGLE_SUCCESS;

        } 
	    
	    private static int runPause(CommandContext<CommandSourceStack> ctx) {
	        CommandSourceStack source = ctx.getSource();

	        if (!(source.getEntity() instanceof ServerPlayer player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.pause.onlyplayer"));
	            return 0;
	        }

	        if (!ViaductTravel.isTravelActive(player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.pause.noactivetravel"));
	            return 0;
	        }

	        TravelData data = ViaductTravel.getTravelData(player);
	        if (data == null) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.pause.nodata"));
	            return 0;
	        }

	        if (data.isPaused) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.pause.alreadypaused"));
	            return 0;
	        }

	        data.isPaused = true;
	        source.sendSuccess(() -> Component.translatable("pipecraft.commands.travel.pause.success"), false);
	        return Command.SINGLE_SUCCESS;
	    }

	    private static int runResume(CommandContext<CommandSourceStack> ctx) {
	        CommandSourceStack source = ctx.getSource();

	        if (!(source.getEntity() instanceof ServerPlayer player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.resume.onlyplayer"));
	            return 0;
	        }

	        if (!ViaductTravel.isTravelActive(player)) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.resume.noactivetravel"));
	            return 0;
	        }

	        TravelData data = ViaductTravel.getTravelData(player);
	        if (data == null) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.resume.nodata"));
	            return 0;
	        }

	        if (!data.isPaused) {
	            source.sendFailure(Component.translatable("pipecraft.commands.travel.resume.notpaused"));
	            return 0;
	        }

	        data.isPaused = false;
	        source.sendSuccess(() -> Component.translatable("pipecraft.commands.travel.resume.success"), false);
	        return Command.SINGLE_SUCCESS;
	    }
	
    
	}