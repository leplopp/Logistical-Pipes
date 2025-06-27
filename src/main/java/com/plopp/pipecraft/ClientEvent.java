package com.plopp.pipecraft;

import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductAdvanced;
import com.plopp.pipecraft.obj.BlockViaductAdvancedShapes;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvent {
	
    @SuppressWarnings("deprecation")
	@SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
         BlockViaductAdvancedShapes.loadAll(BlockViaductAdvanced.objParser);
         ItemBlockRenderTypes.setRenderLayer(BlockRegister.VIADUCT.get(), RenderType.translucent());
 
    }
    
	

}
