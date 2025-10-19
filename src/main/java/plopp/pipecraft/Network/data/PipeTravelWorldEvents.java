package plopp.pipecraft.Network.data;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.logic.pipe.PipeTravel;
import plopp.pipecraft.logic.pipe.TravellingItem;

@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class PipeTravelWorldEvents {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        PipeTravelWorldData data = PipeTravelWorldData.get(level);
        for (TravellingItemRecord rec : data.getItems()) {
            TravellingItem item = new TravellingItem(
                rec.stack(),
                rec.currentPos(),
                rec.side(),
                PipeConfig.defaultConfig(), // falls du Configs je nach Pipe ändern willst → anpassen
                level
            );
            item.lastPos = rec.lastPos();
            item.progress = rec.progress();
            item.speed = rec.speed();
            PipeTravel.activeItems.add(item);
        }

        // Liste leeren, sonst wird beim nächsten Speichern doppelt gespeichert
        data.getItems().clear();
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        PipeTravelWorldData data = PipeTravelWorldData.get(level);
        data.setItems(PipeTravel.activeItems);
    }
}