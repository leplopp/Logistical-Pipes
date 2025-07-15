package plopp.pipecraft.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;

public class DynamicColorWrappedModel implements BakedModel {
	
    private final Map<DyeColor, BakedModel> colorModels;
    private final BakedModel fallback;
    private final ModelState modelState; // hinzufügen
    private static final ModelProperty<DyeColor> COLOR_MODEL_DATA_KEY = new ModelProperty<>();
    private final Map<DyeColor, TextureAtlasSprite> particleSprites;
    
    public DynamicColorWrappedModel(Function<ResourceLocation, BakedModel> modelGenerator,
            Map<DyeColor, ResourceLocation> colorTextures,
            BakedModel fallback,
            ModelState modelState,
            Map<DyeColor, TextureAtlasSprite> particleSprites) {

    	
    	this.fallback = fallback;
        this.modelState = modelState;
        this.particleSprites = particleSprites;
        this.colorModels = new EnumMap<>(DyeColor.class);
        
        for (Map.Entry<DyeColor, ResourceLocation> entry : colorTextures.entrySet()) {
            this.colorModels.put(entry.getKey(), modelGenerator.apply(entry.getValue()));
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (state != null && state.hasProperty(BlockViaduct.COLOR)) {
            DyeColor color = state.getValue(BlockViaduct.COLOR);
            BakedModel model = colorModels.getOrDefault(color, fallback);
            return model.getQuads(state, side, rand);
        }
        return fallback.getQuads(state, side, rand);
    }
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.translucent());
    }
    @Override
    public ItemTransforms getTransforms() {
        return fallback.getTransforms();
    }
    
    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (state != null && state.hasProperty(BlockViaduct.COLOR)) {
            DyeColor color = state.getValue(BlockViaduct.COLOR);
            return modelData.derive().with(COLOR_MODEL_DATA_KEY, color).build();
        }
        return modelData;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        DyeColor color = data.get(COLOR_MODEL_DATA_KEY);
        if (color != null && particleSprites.containsKey(color)) {
            return particleSprites.get(color);
        }
        return fallback.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        TextureAtlasSprite sprite = particleSprites.get(DyeColor.WHITE);
        if (sprite == null) {
            return fallback.getParticleIcon();
        }
        return sprite;
    }
    // Delegiere restliche Methoden wie gehabt
    @Override public boolean useAmbientOcclusion() { return fallback.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return fallback.isGui3d(); }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return fallback.isCustomRenderer(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }
}