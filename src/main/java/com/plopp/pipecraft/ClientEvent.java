package com.plopp.pipecraft;

import org.lwjgl.glfw.GLFW;

import com.plopp.pipecraft.Blocks.Viaduct.BlockViaduct;
import com.plopp.pipecraft.Blocks.Viaduct.BlockViaductAdvanced;
import com.plopp.pipecraft.logic.ViaductTravel;
import com.plopp.pipecraft.obj.BlockViaductAdvancedShapes;
import com.plopp.pipecraft.obj.BlockViaductShapes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvent {
	
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
         BlockViaductShapes.loadAll(BlockViaduct.objParser);
         BlockViaductAdvancedShapes.loadAll(BlockViaductAdvanced.objParser);
 
    }
	

}
