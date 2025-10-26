package plopp.pipecraft.util;

import plopp.pipecraft.logic.ViaductTravel;
import plopp.pipecraft.util.MapInspector;


public class MapInspector {

    public static void inspectTravelMaps() {
        System.out.println("ActiveTravels: " + ViaductTravel.activeTravels.size());
        System.out.println("TravelYawMap: " + ViaductTravel.travelYawMap.size());
        System.out.println("TravelPitchMap: " + ViaductTravel.travelPitchMap.size());
        System.out.println("VerticalDirMap: " + ViaductTravel.verticalDirMap.size());
        System.out.println("ResetModelSet: " + ViaductTravel.resetModelSet.size());
    }
}