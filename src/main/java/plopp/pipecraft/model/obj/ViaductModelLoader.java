package plopp.pipecraft.model.obj;

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
        
        ResourceLocation particleTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        JsonObject texObj = GsonHelper.getAsJsonObject(json, "textures");

        for (Map.Entry<String, JsonElement> entry : texObj.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue().getAsString();

            if (key.equals("particle")) {
                particleTexture = ResourceLocation.parse(val);
            } else {
                try {
                    DyeColor dye = DyeColor.valueOf(key.toUpperCase(Locale.ROOT));
                    colorTextures.put(dye, ResourceLocation.parse(val));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return new ViaductModelGeometry(model, colorTextures, particleTexture);
    }
}