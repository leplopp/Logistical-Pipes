package com.plopp.pipecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.plopp.pipecraft.Blocks.BlockRegister;
import com.plopp.pipecraft.Blocks.ModBlockEntities;
import com.plopp.pipecraft.Blocks.Viaduct.BlockViaduct;
import com.plopp.pipecraft.Blocks.Viaduct.BlockViaductAdvanced;
import com.plopp.pipecraft.obj.BlockViaductAdvancedShapes;
import com.plopp.pipecraft.obj.BlockViaductShapes;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(PipeCraftIndex.MODID)
public class PipeCraftIndex
{
    public static final String MODID = "logisticpipes";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public PipeCraftIndex(IEventBus modEventBus, ModContainer modContainer)
    {

        NeoForge.EVENT_BUS.register(this);
        
        BlockRegister.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        PipeCreativeModeTab.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event){
       
    }
    
    
    private void addCreative(BuildCreativeModeTabContentsEvent event){
    	
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
             BlockViaductShapes.loadAll(BlockViaduct.objParser);
             BlockViaductAdvancedShapes.loadAll(BlockViaductAdvanced.objParser);
     
        }
        
        
      /*  
        @SubscribeEvent
        public static void onModelBake(ModelEvent.ModifyBakingResult event) {
            Map<String, BakedModel> partModels = new HashMap<>();

            // Lade alle Teilmodelle, wie sie in deiner JSON verwendet werden:
            String[] keys = {
            		 "viaduct", "viaduct_connected", "viaduct_connected_corner", "viaduct_connected_long",
            		    "viaduct_connected_side", "viaduct_connected_all_side", "viaduct_connected_cross"
            };

            for (String key : keys) {
                ResourceLocation resLoc = ResourceLocation.tryBuild("logisticpipes", "block/viaduct/" + key);
                if (resLoc != null) {
                    ModelResourceLocation modelLoc = new ModelResourceLocation(resLoc, "normal");
                    BakedModel model = event.getModels().get(modelLoc);
                    if (model != null) {
                        partModels.put(key, model);
                    }
                } else {
                    System.err.println("Ungültige ResourceLocation für: " + key);
                }
            }

            ResourceLocation baseResLoc = ResourceLocation.tryBuild("logisticpipes", "block/viaduct");
            if (baseResLoc != null) {
                ModelResourceLocation baseModelLoc = new ModelResourceLocation(baseResLoc, "normal");
                BakedModel original = event.getModels().get(baseModelLoc);
                if (original != null) {
                    ViaductBakedModel customModel = new ViaductBakedModel(partModels, original);
                    event.getModels().put(baseModelLoc, customModel);
                }
            } else {
                System.err.println("Ungültige ResourceLocation für das Basismodell");
            }
        }*/

    }
}
