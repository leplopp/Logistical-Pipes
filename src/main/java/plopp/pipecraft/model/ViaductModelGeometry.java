package plopp.pipecraft.model;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.obj.ObjLoader;
import net.neoforged.neoforge.client.model.obj.ObjModel;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@OnlyIn(Dist.CLIENT)
public class ViaductModelGeometry implements IUnbakedGeometry<ViaductModelGeometry> {

    private final ResourceLocation objModel;
    private final Map<DyeColor, ResourceLocation> colorTextures;

    public ViaductModelGeometry(ResourceLocation objModel, Map<DyeColor, ResourceLocation> colorTextures) {
        this.objModel = objModel;
        this.colorTextures = colorTextures;
    }


    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                           Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState, ItemOverrides overrides) {

        // Funktion zum Baken eines Modells mit einer spezifischen Textur
        Function<ResourceLocation, BakedModel> modelGenerator = (ResourceLocation texture) -> {
            Function<Material, TextureAtlasSprite> wrappedGetter = mat -> {
                // Ersetze spezielle Platzhalter-Texturen durch die farbspezifische Textur
                if ("placeholder".equals(mat.texture().getPath())) {
                    return spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, texture));
                }
                return spriteGetter.apply(mat);
            };

            ObjModel.ModelSettings settings = new ObjModel.ModelSettings(objModel, true, true, false, true, null);
            ObjModel model = ObjLoader.INSTANCE.loadModel(settings);
            return model.bake(context, baker, wrappedGetter, modelState, overrides);
        };

        // Base-Model (z. B. white oder fallback)
        ResourceLocation whiteTex = colorTextures.getOrDefault(DyeColor.WHITE, ResourceLocation.fromNamespaceAndPath("minecraft", "missingno"));
        BakedModel fallbackModel = modelGenerator.apply(whiteTex);

        Map<DyeColor, TextureAtlasSprite> particleSprites = new EnumMap<>(DyeColor.class);
        for (Map.Entry<DyeColor, ResourceLocation> entry : colorTextures.entrySet()) {
            ResourceLocation tex = entry.getValue();
            TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, tex));
            particleSprites.put(entry.getKey(), sprite);
        }

        return new DynamicColorWrappedModel(modelGenerator, colorTextures, fallbackModel, modelState, particleSprites);
    }
}