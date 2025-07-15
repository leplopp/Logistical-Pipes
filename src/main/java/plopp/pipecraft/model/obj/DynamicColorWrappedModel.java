package plopp.pipecraft.model.obj;

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
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;

public class DynamicColorWrappedModel implements BakedModel {

    private final Map<DyeColor, BakedModel> colorModels;
    private final BakedModel fallback;
    private final TextureAtlasSprite particleSprite;

    public static final ModelProperty<DyeColor> COLOR_MODEL_DATA_KEY = new ModelProperty<>();

    public DynamicColorWrappedModel(Function<ResourceLocation, BakedModel> modelGenerator,
                                    Map<DyeColor, ResourceLocation> colorTextures,
                                    BakedModel fallback,
                                    ModelState modelState,
                                    Map<DyeColor, TextureAtlasSprite> particleSprites,
                                    TextureAtlasSprite particleSprite) {
        this.fallback = fallback;
        this.particleSprite = particleSprite;
        this.colorModels = new EnumMap<>(DyeColor.class);

        for (Map.Entry<DyeColor, ResourceLocation> entry : colorTextures.entrySet()) {
            this.colorModels.put(entry.getKey(), modelGenerator.apply(entry.getValue()));
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    net.neoforged.neoforge.client.model.data.ModelData modelData,
                                    RenderType renderType) {
        DyeColor color = extractColor(state, modelData);
        BakedModel model = color != null ? colorModels.getOrDefault(color, fallback) : fallback;
        return model.getQuads(state, side, rand, modelData, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, null, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        boolean translucent = state != null
            && ((state.hasProperty(BlockViaduct.TRANSPARENT) && state.getValue(BlockViaduct.TRANSPARENT)) ||
                (state.hasProperty(BlockViaductLinker.TRANSPARENT) && state.getValue(BlockViaductLinker.TRANSPARENT)));
        return ChunkRenderTypeSet.of(translucent ? RenderType.translucent() : RenderType.cutout());
    }
    
    @Override
    public ItemTransforms getTransforms() {
        return fallback.getTransforms();
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        if (state != null) {
            if (state.hasProperty(BlockViaduct.COLOR)) {
                return data.derive().with(COLOR_MODEL_DATA_KEY, state.getValue(BlockViaduct.COLOR)).build();
            } else if (state.hasProperty(BlockViaductLinker.COLOR)) {
                return data.derive().with(COLOR_MODEL_DATA_KEY, state.getValue(BlockViaductLinker.COLOR)).build();
            }
        }
        return data;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return particleSprite;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particleSprite;
    }

    private DyeColor extractColor(@Nullable BlockState state, @Nullable ModelData modelData) {
        if (state != null) {
            if (state.hasProperty(BlockViaduct.COLOR)) return state.getValue(BlockViaduct.COLOR);
            if (state.hasProperty(BlockViaductLinker.COLOR)) return state.getValue(BlockViaductLinker.COLOR);
        }
        return modelData != null ? modelData.get(COLOR_MODEL_DATA_KEY) : null;
    }

    @Override public boolean useAmbientOcclusion() { return fallback.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return fallback.isGui3d(); }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return fallback.isCustomRenderer(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }
}