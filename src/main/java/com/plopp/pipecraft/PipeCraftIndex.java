package com.plopp.pipecraft;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.plopp.pipecraft.Blocks.BlockRegister;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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
        
    
}
