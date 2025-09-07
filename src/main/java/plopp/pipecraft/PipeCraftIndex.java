package plopp.pipecraft;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.gui.MenuTypeRegister;

@Mod(PipeCraftIndex.MODID)
public class PipeCraftIndex
{
    public static final String MODID = "logisticpipes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PipeCraftIndex(IEventBus modEventBus, ModContainer modContainer)
    {

        NeoForge.EVENT_BUS.register(this);
        BlockRegister.register(modEventBus);
        PipeCreativeModeTab.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        BlockEntityRegister.register(modEventBus);
        MenuTypeRegister.MENUS.register(modEventBus);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event){
    	
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }
}

/*ideen
 * 
 *  viaduct											<- Pre Release				/ hitbox vollenden /sounds hinzufügen
 *  viaduct Linker									<- Pre Release				/ hitbox vollenden /sounds hinzufügen 
 *  viaduct	glowing option 							<- Released					
 *  viaduct player detector 					    <- Beta						/ render layer activate
 *  viaduct Speed controller						<- Released						
 *  viaduct Teleporter								<- Pre Alpha 				/ ist zu komplex 
 *   	
 *  viaduct facade for all pipes & cables			<- concept
 *   
 *  item & fluid pipes: 							
 *   
 *  pipe											<- Pre Alpha
 *  sorter pipe										<- concept
 *  vacuum pipe										<- concept
 *  speed up pipe									<- concept
 *  detector pipe									<- concept
 *  
 *  item schlömpel									<- concept
 *  multi schlömpel									<- concept
 *  Fluid Schlömpel									<- concept
 *  
 */
