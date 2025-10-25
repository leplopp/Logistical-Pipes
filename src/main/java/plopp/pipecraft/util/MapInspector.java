package plopp.pipecraft.util;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import java.lang.reflect.Field;
import java.util.Optional;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.util.MapInspector;


public class MapInspector {

    public static void countAllModEntries(String modid) {
        Field[] fields = NeoForgeRegistries.class.getDeclaredFields();

        for (Field field : fields) {
            if (!Registry.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            try {
                Registry<?> registry = (Registry<?>) field.get(null);
                @SuppressWarnings("unchecked")
				long count = registry.stream()
                        .filter(obj -> {
                            Optional<ResourceKey<Object>> keyOpt = ((Registry<Object>)registry).getResourceKey((Object)obj);
                            return keyOpt.isPresent() && keyOpt.get().location().getNamespace().equals(modid);
                        })
                        .count();
                if (count > 0) {
                    System.out.println("Registry " + field.getName() + " hat " + count + " Einträge von " + modid);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void inspectTravelMaps() {
        System.out.println("ActiveTravels: " + ViaductTravel.activeTravels.size());
        System.out.println("TravelYawMap: " + ViaductTravel.travelYawMap.size());
        System.out.println("TravelPitchMap: " + ViaductTravel.travelPitchMap.size());
        System.out.println("VerticalDirMap: " + ViaductTravel.verticalDirMap.size());
        System.out.println("ResetModelSet: " + ViaductTravel.resetModelSet.size());
    }
    
}