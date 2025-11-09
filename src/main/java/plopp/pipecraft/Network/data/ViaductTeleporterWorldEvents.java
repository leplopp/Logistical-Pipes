package plopp.pipecraft.Network.data;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductTeleporter;
import plopp.pipecraft.Network.teleporter.TeleporterEntryRecord;
import plopp.pipecraft.Network.teleporter.ViaductTeleporterIdRegistry;
import plopp.pipecraft.logic.Manager.ViaductTeleporterManager;

@EventBusSubscriber(modid = PipeCraftIndex.MODID)
public class ViaductTeleporterWorldEvents {

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel serverLevel))
			return;

		ViaductTeleporterWorldData data = ViaductTeleporterWorldData.get(serverLevel);
		for (TeleporterEntryRecord record : data.getTeleporters().values()) {
			String id = BlockEntityViaductTeleporter.generateItemId(record.start().icon());
			if (!id.isEmpty() && !ViaductTeleporterIdRegistry.isIdTaken(id)) {
				ViaductTeleporterIdRegistry.registerTeleporter(id, record);
			}
		}
		ViaductTeleporterManager.loadFromWorldData(data);
	}

	@SubscribeEvent
	public static void onLevelSave(LevelEvent.Save event) {
		if (!(event.getLevel() instanceof ServerLevel serverLevel))
			return;
		ViaductTeleporterWorldData data = ViaductTeleporterWorldData.get(serverLevel);
		ViaductTeleporterManager.saveToWorldData(data);
	}
}
