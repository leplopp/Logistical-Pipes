package plopp.pipecraft;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.gui.MenuTypeRegister;
import plopp.pipecraft.sounds.SoundRegister;
import plopp.pipecraft.util.DebugTravelCommand;
import plopp.pipecraft.util.MapInspector;

@Mod(PipeCraftIndex.MODID)
public class PipeCraftIndex
{
    public static final String MODID = "logisticpipes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PipeCraftIndex(IEventBus modEventBus, ModContainer modContainer){

        NeoForge.EVENT_BUS.register(this);
        BlockRegister.register(modEventBus);
        PipeCreativeModeTab.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        BlockEntityRegister.register(modEventBus);
        MenuTypeRegister.MENUS.register(modEventBus);
        SoundRegister.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(DebugTravelCommand::register);
    }

   
    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
        	MapInspector.inspectTravelMaps();
        });
    }
    
    private void addCreative(BuildCreativeModeTabContentsEvent event){}
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event){}
}

/*known bugs: 
 * 
 * 
 * sound plopp multiplayer
 * 
 * 
 * 
 */


/*ideen
 * 
 *  viaduct											<- Pre Release				/ hitbox vollenden
 *  viaduct Linker									<- Pre Release				/ hitbox vollenden
 *  viaduct player detector 					    <- Released						
 *  viaduct Speed controller						<- Released						
 *  viaduct Teleporter								<- Pre Alpha 				/
 *   	
 *  viaduct facade for all pipes & cables			<- Pre Alpha 
 *   
 *  item & fluid pipes: 							
 *   
 *  pipe		 	/ iron						    <- Beta
 *  extractor pipe  / iron						    <- Beta
 *  sorter pipe	/ diamond							<- concept		
 *  speed up pipe	/gold							<- concept
 *  detector pipe	/iron							<- concept
 *	Direction blocker pipe	/ iron					<- concept
 *  
 */
