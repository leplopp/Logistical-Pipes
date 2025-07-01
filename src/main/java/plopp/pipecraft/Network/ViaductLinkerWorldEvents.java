package plopp.pipecraft.Network;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.logic.ViaductLinkerManager;

@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class ViaductLinkerWorldEvents {
	
	 @SubscribeEvent
	    public static void onLevelLoad(LevelEvent.Load event) {
	        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
	        ViaductLinkerWorldData data = ViaductLinkerWorldData.get(serverLevel);
	        ViaductLinkerManager.loadFromWorldData(data);
	    }

	    @SubscribeEvent
	    public static void onLevelSave(LevelEvent.Save event) {
	        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
	        ViaductLinkerWorldData data = ViaductLinkerWorldData.get(serverLevel);
	        ViaductLinkerManager.saveToWorldData(data);
	    }
}
