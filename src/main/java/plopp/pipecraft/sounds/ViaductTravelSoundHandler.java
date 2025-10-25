package plopp.pipecraft.sounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Network.travel.ClientTravelDataManager;

@EventBusSubscriber(modid = PipeCraftIndex.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ViaductTravelSoundHandler {
	private static final Map<UUID, ViaductLoopSound> loopMap = new HashMap<>();

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		double maxDistanceSqr = 32*32;

		for (Player player : mc.level.players()) {
		    UUID uuid = player.getUUID();
		    boolean active = ClientTravelDataManager.isTravelActive(uuid);

		    ViaductLoopSound loop = loopMap.get(uuid);

		    if (active) {
		        double distanceSqr = mc.player.distanceToSqr(player);
		        if (distanceSqr > maxDistanceSqr) {
		            if (loop != null) {
		                mc.getSoundManager().stop(loop);
		                loopMap.remove(uuid);
		            }
		            continue;
		        }

		        if (loop == null || !mc.getSoundManager().isActive(loop)) {
		            loop = new ViaductLoopSound(player);
		            loopMap.put(uuid, loop);
		            mc.getSoundManager().play(loop);
		        }
		    } else if (loop != null) {
		        mc.getSoundManager().stop(loop);
		        loopMap.remove(uuid);
		    }
		}
	}
}