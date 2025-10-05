package plopp.pipecraft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import plopp.pipecraft.logic.SpeedLevel;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD)

public class Config {

    public static final ModConfigSpec SPEC;  
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	
    // Minimal: nur Speed Viaduct Range
    private static final ModConfigSpec.IntValue SPEED_VIADUCT_MIN = BUILDER
            .comment("Minimum speed level for Speed Viaduct (1-128)")
            .defineInRange("speedViaductMin", 1, 1, 128);

    private static final ModConfigSpec.IntValue SPEED_VIADUCT_MAX = BUILDER
            .comment("Maximum speed level for Speed Viaduct (1-128)")
            .defineInRange("speedViaductMax", 128, 1, 128);
    
    static {
        SPEC = BUILDER.build();
    }

    public static int speedViaductMin;
    public static int speedViaductMax;
    // Eigenen NeoForge-Typ definieren

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        speedViaductMin = SPEED_VIADUCT_MIN.get();
        speedViaductMax = SPEED_VIADUCT_MAX.get();

        // Sicherstellen, dass min <= max
        if (speedViaductMin > speedViaductMax) {
            int tmp = speedViaductMin;
            speedViaductMin = speedViaductMax;
            speedViaductMax = tmp;
        }

        // Extra: Werte fixen, falls aus der Config außerhalb des Bereichs
        if (speedViaductMin < 1) speedViaductMin = 1;
        if (speedViaductMax > 128) speedViaductMax = 128;
    
    }

    /** Hilfsmethode: prüfen, ob ein SpeedLevel erlaubt ist */
    public static boolean isSpeedLevelAllowed(SpeedLevel level) {
        return level.getValue() >= speedViaductMin && level.getValue() <= speedViaductMax;
    }
}
