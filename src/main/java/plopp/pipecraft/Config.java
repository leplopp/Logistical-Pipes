package plopp.pipecraft;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import plopp.pipecraft.logic.SpeedLevel;

public class Config {

	public static final ModConfigSpec SPEC;
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	private static final ModConfigSpec.IntValue SPEED_VIADUCT_MIN;
	private static final ModConfigSpec.IntValue SPEED_VIADUCT_MAX;
	private static final ModConfigSpec.IntValue SCAN_SPEED;
	private static final BooleanValue ENABLE_VIADUCT_TELEPORTER;
	private static final BooleanValue ENABLE_VIADUCT_FACADE;
	private static final BooleanValue ENABLE_BLOCKPIPE;
	private static final BooleanValue ENABLE_BLOCKPIPE_EXTRACT;
	public static final ConfigValue<List<? extends String>> FACADABLE_BLOCKS;
	
	static {

		BUILDER.push("Viaduct Booster");
		BUILDER.comment("");
		BUILDER.comment("Default main speed is 32 = 32 Ticks per chunk, please use faster time than 6 cearfully.\n"
				+ "on Servers or Client without C2ME (to speed up chunk loadung)");
		BUILDER.comment("");
		SPEED_VIADUCT_MIN = BUILDER

				.comment("Minimum speed level for Speed Viaduct (1-128) e.g Faster")
				.defineInRange("speedViaductMin", 6, 1, 128);

		BUILDER.comment("");

		SPEED_VIADUCT_MAX = BUILDER.comment("Maximum speed level for Speed Viaduct (128-1) e.g Slower")
				.defineInRange("speedViaductMax", 128, 1, 128);
		BUILDER.pop();

		BUILDER.push("Viaduct Connector");
		BUILDER.comment("");
		 BUILDER.comment(
				"change the scann speed e.g 512 = 512 blocks per tick, default is 99 for best performance and speed,\n "
						+ "set this lower to slow down the scan speed and make scan performance better");
		 BUILDER.comment("");		
		 SCAN_SPEED =BUILDER.comment("Scan speed (steps per tick) for AsyncViaductScanner. Default: 99, range: 1–512.")
				.defineInRange("scanSpeed", 99, 1, 512);

		BUILDER.pop();
		
		BUILDER.push("Experimental Content");
		BUILDER.comment("");
		BUILDER.comment("Status = BETA");
		BUILDER.comment("");
		ENABLE_VIADUCT_TELEPORTER = BUILDER
		    .comment("Enable the Viaduct Teleporter block in the creative tab (experimental).")
		    .define("enableViaductTeleporter", false);
		BUILDER.comment("");
		BUILDER.comment("Status = ALPHA");
		BUILDER.comment("");
		ENABLE_VIADUCT_FACADE = BUILDER
		    .comment("Enable the Viaduct Facade block in the creative tab (experimental).")
		    .define("enableViaductFacade", false);
		BUILDER.comment("");
		BUILDER.comment("Status = BETA");
		BUILDER.comment("");
		ENABLE_BLOCKPIPE = BUILDER
		    .comment("Enable the Block Pipe in the creative tab (experimental).")
		    .define("enableBlockPipe", false);
		BUILDER.comment("");
		BUILDER.comment("Status = PRE ALPHA");
		BUILDER.comment("");
		ENABLE_BLOCKPIPE_EXTRACT = BUILDER
		    .comment("Enable the Pipe Extractor block in the creative tab (experimental).")
		    .define("enableBlockPipeExtract", false);

		BUILDER.pop();	

		BUILDER.push("Allowed Facades");
		BUILDER.comment("");
		FACADABLE_BLOCKS = BUILDER.comment("List of blocks that can accept a Viaduct Facade")
			    .defineList(
			        "facadableBlocks",
			        List.of(
		            "mekanism:basic_logistical_transporter",
		            "mekanism:advanced_logistical_transporter",
		            "mekanism:elite_logistical_transporter",
		            "mekanism:ultimate_logistical_transporter",
		            "mekanism:restrictive_transporter",
		            "mekanism:diversion_transporter",
		            "mekanism:ultimate_logistical_transporter",
		            "mekanism:basic_mechanical_transporter",
		            "mekanism:advanced_mechanical_transporter",
		            "mekanism:elite_mechanical_transporter",
		            "mekanism:ultimate_mechanical_transporter",
		            "mekanism:basic_universal_cable",
		            "mekanism:advanced_universal_cable",
		            "mekanism:elite_universal_cable",
		            "mekanism:ultimate_universal_cable",
		            "mekanism:basic_pressurized_tube",
		            "mekanism:advanced_pressurized_tube",
		            "mekanism:elite_pressurized_tube",
		            "mekanism:ultimate_pressurized_tube",
		            "mekanism:basic_thermodynamic_conductor",
		            "mekanism:advanced_thermodynamic_conductor",
		            "mekanism:elite_thermodynamic_conductor",
		            "mekanism:ultimate_thermodynamic_conductor",
		            "mekanism:logistical_sorter",
		            "logisticpipes:pipe",
		            "logisticpipes:pipe_extractor"
			        		  ),
			        () -> "MODID:BLOCK",
			        o -> o instanceof String
			    );
		    BUILDER.pop();
		
		SPEC = BUILDER.build();

	}

	public static int getSpeedViaductMin() {
		return SPEED_VIADUCT_MIN.get();
	}

	public static int getSpeedViaductMax() {
		return SPEED_VIADUCT_MAX.get();
	}

	public static int getScanSpeed() {
		return SCAN_SPEED.get();
	}
	
	public static boolean isViaductTeleporterEnabled() {
	    return ENABLE_VIADUCT_TELEPORTER.get();
	}

	public static boolean isViaductFacadeEnabled() {
	    return ENABLE_VIADUCT_FACADE.get();
	}

	public static boolean isBlockPipeEnabled() {
	    return ENABLE_BLOCKPIPE.get();
	}

	public static boolean isBlockPipeExtractEnabled() {
	    return ENABLE_BLOCKPIPE_EXTRACT.get();
	}
	
	public static Set<ResourceLocation> getFacadableBlocks() {
	    return FACADABLE_BLOCKS.get().stream()
	        .map(s -> {
	            String str = (String) s;
	            String[] parts = str.split(":", 2); 
	            String namespace = parts.length > 1 ? parts[0] : "minecraft"; 
	            String path = parts.length > 1 ? parts[1] : parts[0];
	            return ResourceLocation.fromNamespaceAndPath(namespace, path);
	        })
	        .collect(Collectors.toSet());
	}

	public static boolean isSpeedLevelAllowed(SpeedLevel level) {
		return level.getValue() >= getSpeedViaductMin() && level.getValue() <= getSpeedViaductMax();
	}
}