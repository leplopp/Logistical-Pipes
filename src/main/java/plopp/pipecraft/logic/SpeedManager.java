package plopp.pipecraft.logic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SpeedManager {

	 private static final Map<Item, SpeedLevel> GLOBAL_SPEEDS = new ConcurrentHashMap<>();

	    public static void setSpeed(ItemStack stack, SpeedLevel speed) {
	        if (!stack.isEmpty()) {
	            GLOBAL_SPEEDS.put(stack.getItem(), speed);
	        }
	    }

	    public static SpeedLevel getSpeed(ItemStack stack) {
	        if (stack.isEmpty()) return null;
	        return GLOBAL_SPEEDS.get(stack.getItem());
	    }
	}