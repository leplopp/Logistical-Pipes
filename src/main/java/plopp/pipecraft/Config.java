package plopp.pipecraft;

import net.neoforged.neoforge.common.ModConfigSpec;
import plopp.pipecraft.logic.SpeedLevel;

public class Config {

	public static final ModConfigSpec SPEC;
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	private static final ModConfigSpec.IntValue SPEED_VIADUCT_MIN;
	private static final ModConfigSpec.IntValue SPEED_VIADUCT_MAX;
	private static final ModConfigSpec.IntValue SCAN_SPEED;

	static {

		BUILDER.push("Viaduct Booster");
		BUILDER.comment("");
		BUILDER.comment("Default main speed is 32 = 32 Ticks per chunk, please use faster time than 6 cearfully.\n"
				+ "on Servers or Client without C2ME (to speed up chunk loadung) its better to set this to ~10");
		BUILDER.comment("");
		SPEED_VIADUCT_MIN = BUILDER

				.comment("Minimum speed level for Speed Viaduct (1-128) e.g Faster")
				.defineInRange("speedViaductMin", 6, 1, 128);

		BUILDER.comment("");

		SPEED_VIADUCT_MAX = BUILDER.comment("Maximum speed level for Speed Viaduct (128-1) e.g Slower")
				.defineInRange("speedViaductMax", 128, 1, 128);
		BUILDER.pop();

		BUILDER.push("Viaduct Connector");

		SCAN_SPEED = BUILDER.comment(
				"change the scann speed e.g 512 = 512 blocks per tick, default is 99 for best performance and speed,\n "
						+ "set this lower to slow down the scan speed and make scan performance better")
				.comment("Scan speed (steps per tick) for AsyncViaductScanner. Default: 99, range: 1–512.")
				.defineInRange("scanSpeed", 99, 1, 512);

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

	public static boolean isSpeedLevelAllowed(SpeedLevel level) {
		return level.getValue() >= getSpeedViaductMin() && level.getValue() <= getSpeedViaductMax();
	}
}
