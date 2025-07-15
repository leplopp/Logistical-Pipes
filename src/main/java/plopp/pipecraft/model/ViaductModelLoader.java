package plopp.pipecraft.model;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public class ViaductModelLoader implements IGeometryLoader<ViaductModelGeometry> {

    public static final ViaductModelLoader INSTANCE = new ViaductModelLoader();

    @Override
    public ViaductModelGeometry read(JsonObject json, JsonDeserializationContext ctx) {
        ResourceLocation model = ResourceLocation.parse(GsonHelper.getAsString(json, "model"));

        Map<DyeColor, ResourceLocation> colorTextures = new EnumMap<>(DyeColor.class);
        JsonObject texObj = GsonHelper.getAsJsonObject(json, "textures");

        for (Map.Entry<String, JsonElement> entry : texObj.entrySet()) {
            try {
                DyeColor dyeColor = DyeColor.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                colorTextures.put(dyeColor, ResourceLocation.parse(entry.getValue().getAsString()));
            } catch (IllegalArgumentException ignored) {
                // Falls kein gültiger Farbname, einfach überspringen
            }
        }

        return new ViaductModelGeometry(model, colorTextures);
    }
}