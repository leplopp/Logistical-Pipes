package com.plopp.pipecraft.Network;

import com.plopp.pipecraft.PipeCraftIndex;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;
import com.plopp.pipecraft.logic.ViaductLinkerManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

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
