package com.plopp.pipecraft;

import com.plopp.pipecraft.Blocks.Viaduct.BlockViaduct;
import com.plopp.pipecraft.Blocks.Viaduct.BlockViaductAdvanced;
import com.plopp.pipecraft.obj.BlockViaductAdvancedShapes;
import com.plopp.pipecraft.obj.BlockViaductShapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvent {
	
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
         BlockViaductShapes.loadAll(BlockViaduct.objParser);
         BlockViaductAdvancedShapes.loadAll(BlockViaductAdvanced.objParser);
 
    }
    
    
	

}
