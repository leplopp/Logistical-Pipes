package plopp.pipecraft.logic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import plopp.pipecraft.Config;

public class SpeedManager {

	private static final Map<Item, SpeedLevel> GLOBAL_SPEEDS = new ConcurrentHashMap<>();

	public static void setSpeed(ItemStack stack, SpeedLevel speed) {
		if (stack.isEmpty())
			return;

		if (!Config.isSpeedLevelAllowed(speed)) {

			int clamped = Math.max(Config.getSpeedViaductMin(),
					Math.min(Config.getSpeedViaductMax(), speed.getValue()));
			speed = SpeedLevel.fromInt(clamped);
		}

		GLOBAL_SPEEDS.put(stack.getItem(), speed);
	}

	public static SpeedLevel getSpeed(ItemStack stack) {
		if (stack.isEmpty())
			return null;

		SpeedLevel speed = GLOBAL_SPEEDS.get(stack.getItem());
		if (speed == null)
			return null;

		if (!Config.isSpeedLevelAllowed(speed)) {
			int clamped = Math.max(Config.getSpeedViaductMin(),
					Math.min(Config.getSpeedViaductMax(), speed.getValue()));
			speed = SpeedLevel.fromInt(clamped);
			GLOBAL_SPEEDS.put(stack.getItem(), speed);
		}

		return speed;
	}
}