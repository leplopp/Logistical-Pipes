package plopp.pipecraft.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.logic.ViaductTravel;

//@EventBusSubscriber(modid = PipeCraftIndex.MODID, value = Dist.CLIENT)
public class ViaductModelFix {

	/*  private static final Map<UUID, Float> previousBodyRot = new HashMap<>();
	    private static final Map<UUID, Float> previousHeadRot = new HashMap<>();
	    
	    @SubscribeEvent
	    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
	        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();

	        if (ViaductTravel.isTravelActive(player)) {
	            float travelYaw = ViaductTravel.getTravelYaw(player.getUUID());

	            // Backup
	            previousBodyRot.put(player.getUUID(), player.yBodyRot);
	            previousHeadRot.put(player.getUUID(), player.yHeadRot);

	            // Erzwinge "Blickrichtung" für Modell, unabhängig von Kamera
	            player.yBodyRot = travelYaw;
	            player.yHeadRot = travelYaw;
	        }
	    }

	    @SubscribeEvent
	    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
	        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();

	        if (ViaductTravel.isTravelActive(player)) {
	            // Wiederherstellen der echten Rotationen
	            player.yBodyRot = previousBodyRot.getOrDefault(player.getUUID(), player.yBodyRot);
	            player.yHeadRot = previousHeadRot.getOrDefault(player.getUUID(), player.yHeadRot);
	        }
	    }*/
}
