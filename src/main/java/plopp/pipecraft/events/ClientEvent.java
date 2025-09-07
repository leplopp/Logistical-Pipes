package plopp.pipecraft.events;

import java.lang.reflect.Field;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.gui.MenuTypeRegister;
import plopp.pipecraft.gui.teleporter.ViaductTeleporterScreen;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerIDScreen;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerScreen;
import plopp.pipecraft.model.LyingPlayerModel;
//import plopp.pipecraft.model.LyingPlayerModel;
import plopp.pipecraft.model.obj.ViaductModelLoader;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvent {
	
    @SubscribeEvent
    public static void onClientSetup(RegisterMenuScreensEvent event) {
    	event.register(MenuTypeRegister.VIADUCT_LINKER.get(), ViaductLinkerScreen::new);
    	event.register(MenuTypeRegister.VIADUCT_LINKER_ID.get(), ViaductLinkerIDScreen::new);
    	event.register(MenuTypeRegister.VIADUCT_TELEPORTER.get(), ViaductTeleporterScreen::new);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                boolean slim = skin.name().equals("slim");
                
                ModelPart root = event.getEntityModels().bakeLayer(
                    slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER
                );

                LyingPlayerModel<AbstractClientPlayer> newModel = new LyingPlayerModel<>(root, slim);

                try {
                    Field modelField = LivingEntityRenderer.class.getDeclaredField("model");
                    modelField.setAccessible(true);
                    modelField.set(renderer, newModel);
                } catch (Exception e) {
                    System.err.println("[onAddLayers] Fehler beim Setzen des Modells für Skin " + skin.name());
                    e.printStackTrace();
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
    	  event.register(ResourceLocation.fromNamespaceAndPath("pipecraft", "viaduct_model"), ViaductModelLoader.INSTANCE);
    }
}