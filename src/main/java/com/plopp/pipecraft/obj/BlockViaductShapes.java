package com.plopp.pipecraft.obj;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public class BlockViaductShapes {
	public static List<Triangle> DEFAULT;
    public static List<Triangle> CONNECTED;
    public static List<Triangle> CORNER;
    public static List<Triangle> LONG;

    public static void loadAll(objParser parser) {
        DEFAULT   = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/viaduct.obj"));
        CONNECTED = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/viaduct_connected.obj"));
        CORNER    = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/viaduct_connected_corner.obj"));
        LONG      = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/viaduct_connected_long.obj"));
    }

}
