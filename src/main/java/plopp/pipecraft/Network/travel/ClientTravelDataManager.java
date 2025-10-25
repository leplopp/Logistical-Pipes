package plopp.pipecraft.Network.travel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import plopp.pipecraft.logic.ViaductTravel;

public class ClientTravelDataManager {

	private static final Map<UUID, TravelStatePacket> travelDataMap = new HashMap<>();

	public static void updatePlayerTravelData(TravelStatePacket packet) {
		travelDataMap.put(packet.getPlayerUUID(), packet);
	}

	public static TravelStatePacket getTravelData(UUID playerUUID) {
		return travelDataMap.get(playerUUID);
	}

	public static boolean isTravelActive(UUID uuid) {
		TravelStatePacket packet = travelDataMap.get(uuid);
		return packet != null && packet.isActive();
	}

	public static float getTravelYaw(UUID uuid) {
		TravelStatePacket packet = travelDataMap.get(uuid);
		return packet != null ? packet.getTravelYaw() : 0f;
	}

	public static float getTravelPitch(UUID uuid) {
		TravelStatePacket packet = travelDataMap.get(uuid);
		return packet != null ? packet.getTravelPitch() : 0f;
	}

	public static ViaductTravel.VerticalDirection getVerticalDirection(UUID uuid) {
		TravelStatePacket packet = travelDataMap.get(uuid);
		return packet != null ? packet.getVerticalDirection() : ViaductTravel.VerticalDirection.NONE;
	}

	public static boolean hasData(UUID uuid) {
		return travelDataMap.containsKey(uuid);
	}
}