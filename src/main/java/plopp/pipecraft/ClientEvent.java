package plopp.pipecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductAdvanced;
import plopp.pipecraft.gui.MenuTypeRegister;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerIDScreen;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerScreen;
import plopp.pipecraft.obj.BlockViaductAdvancedShapes;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvent {
	
	@SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
         BlockViaductAdvancedShapes.loadAll(BlockViaductAdvanced.objParser);

    }
    @SubscribeEvent
    public static void onClientSetup(RegisterMenuScreensEvent event) {
    	event.register(MenuTypeRegister.VIADUCT_LINKER.get(), ViaductLinkerScreen::new);
    	event.register(MenuTypeRegister.VIADUCT_LINKER_ID.get(), ViaductLinkerIDScreen::new);
    }

}
