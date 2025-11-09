package plopp.pipecraft.util.Commands;

import java.util.Locale;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Facade.BlockViaductFacade;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.logic.Manager.FacadeOverlayManager;

public class ViaductCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("viaduct")
            // Viaduct
            .then(Commands.literal("viaduct")
                .then(Commands.argument("color", StringArgumentType.word())
                    .executes(ctx -> setBlockColor(ctx, BlockRegister.VIADUCT.get()))))
            // Speedblock
            .then(Commands.literal("speedblock")
                .then(Commands.argument("color", StringArgumentType.word())
                    .executes(ctx -> setBlockColor(ctx, BlockRegister.VIADUCTSPEED.get()))))
            // Linker
            .then(Commands.literal("linker")
                .then(Commands.argument("color", StringArgumentType.word())
                    .executes(ctx -> setBlockColor(ctx, BlockRegister.VIADUCTLINKER.get()))))
            // Detector
            .then(Commands.literal("detector")
                .then(Commands.argument("color", StringArgumentType.word())
                    .executes(ctx -> setBlockColor(ctx, BlockRegister.VIADUCTDETECTOR.get()))))
            // Facade
            .then(Commands.literal("facade")
                .then(Commands.argument("color", StringArgumentType.word())
                    .executes(ctx -> setBlockColor(ctx, BlockRegister.VIADUCTFACADE.get()))))
        );
    }
    
    private static int setBlockColor(CommandContext<CommandSourceStack> ctx, Block block) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        String colorName = StringArgumentType.getString(ctx, "color");
        DyeColor dye;
        try {
            dye = DyeColor.valueOf(colorName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid color: " + colorName));
            return 0;
        }

        BlockState state = level.getBlockState(pos);
        Block blockAtPos = state.getBlock();

        if (block instanceof BlockViaductFacade) {
            if (!level.isClientSide) {
                FacadeOverlayManager.addFacade(pos, dye, false);
               // NetworkHandler.sendFacadeColorUpdateToTracking(level, pos, new FacadeColorUpdatePacket(pos, dye));
            }
            player.displayClientMessage(Component.translatable("viaduct.color_change", dye.getName()), true);
            player.playSound(SoundEvents.DYE_USE, 1f, 2f);

        } else if (blockAtPos instanceof BlockViaductSpeed) {
            level.setBlockAndUpdate(pos, state.setValue(BlockViaductSpeed.COLOR, dye));
        } else if (blockAtPos instanceof BlockViaduct) {
            level.setBlockAndUpdate(pos, state.setValue(BlockViaduct.COLOR, dye));
        } else if (blockAtPos instanceof BlockViaductLinker) {
            level.setBlockAndUpdate(pos, state.setValue(BlockViaductLinker.COLOR, dye));
        } else if (blockAtPos instanceof BlockViaductDetector) {
            level.setBlockAndUpdate(pos, state.setValue(BlockViaductDetector.COLOR, dye));
        } else {
            player.displayClientMessage(Component.literal("This block has no color property!"), true);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Set " + block.getName().getString() + " color to " + dye.getName()), true);
        return 1;
    }
}