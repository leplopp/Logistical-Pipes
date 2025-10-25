package plopp.pipecraft.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DebugTravelCommand {
	
	  public static void register(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
	        event.getDispatcher().register(
	            Commands.literal("inspecttravels")
	                .requires(source -> source.hasPermission(0)) // 0 = alle Spieler, 2 = nur Operatoren
	                .executes(DebugTravelCommand::runInspect)
	        );
	    }

	    private static int runInspect(CommandContext<CommandSourceStack> ctx) {
	        MapInspector.inspectTravelMaps();
	        ctx.getSource().sendSuccess(() -> 
	            net.minecraft.network.chat.Component.literal("Travel maps inspected. Siehe Konsole!"), false
	        );
	        return Command.SINGLE_SUCCESS;
	    }
}
