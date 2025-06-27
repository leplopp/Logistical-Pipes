package com.plopp.pipecraft.obj;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public class BlockViaductAdvancedShapes {
    public static List<Triangle> DEFAULT;
    public static List<Triangle> CONNECTED;
    public static List<Triangle> CORNER;
    public static List<Triangle> LONG;
    public static List<Triangle> ALL_SIDE;
    public static List<Triangle> CROSS;
    public static List<Triangle> SIDE_ROTATED;

    public static void loadAll(objParser parser) {
        DEFAULT   = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/hitbox/box.obj"));
        CONNECTED = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/hitbox/box.obj"));
        CORNER    = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/hitbox/box.obj"));
        LONG      = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/hitbox/box.obj"));
        ALL_SIDE     = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/hitbox/box.obj"));
        CROSS        = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/hitbox/box.obj"));
        SIDE_ROTATED = objParser.loadTrianglesFromObj(ResourceLocation.fromNamespaceAndPath("logisticpipes", "models/block/viaduct/hitbox/box.obj"));
    }

}
