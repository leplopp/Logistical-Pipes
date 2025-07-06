package plopp.pipecraft;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import plopp.pipecraft.Blocks.BlockEntityRegister;
import plopp.pipecraft.Blocks.ViaductOverlayRenderer;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaduct;
import plopp.pipecraft.gui.MenuTypeRegister;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerIDScreen;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerScreen;
import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.model.LyingPlayerModel;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvent {
	
	@SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        	BlockEntityRenderers.register(
        		    (BlockEntityType<BlockEntityViaduct>) BlockEntityRegister.VIADUCT.get(),
        		    (BlockEntityRendererProvider<BlockEntityViaduct>) ViaductOverlayRenderer::new
        		);
        });
	    }
    @SubscribeEvent
    public static void onClientSetup(RegisterMenuScreensEvent event) {
    	event.register(MenuTypeRegister.VIADUCT_LINKER.get(), ViaductLinkerScreen::new);
    	event.register(MenuTypeRegister.VIADUCT_LINKER_ID.get(), ViaductLinkerIDScreen::new);
    	
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
                    e.printStackTrace();
                }
            }
        }
    }
    
  

}

