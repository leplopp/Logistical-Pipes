package plopp.pipecraft.Network.data;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterManager;

@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class ViaductTeleporterWorldEvents {
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        ViaductTeleporterWorldData data = ViaductTeleporterWorldData.get(serverLevel);
        ViaductTeleporterManager.loadFromWorldData(data); 
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        ViaductTeleporterWorldData data = ViaductTeleporterWorldData.get(serverLevel);
        ViaductTeleporterManager.saveToWorldData(data);
    }
}
