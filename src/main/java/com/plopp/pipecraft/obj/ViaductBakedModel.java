package com.plopp.pipecraft.obj;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.plopp.pipecraft.Blocks.Viaduct.BlockViaductAdvanced;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.data.ModelData;

/*public class ViaductBakedModel implements BakedModel {

	private record MatchCondition(boolean up, boolean down, boolean north, boolean south, boolean east, boolean west, boolean corner, String modelKey, int x, int y) {}

	private static final List<MatchCondition> CONDITIONS = List.of(
	    new MatchCondition(false, false, true, false, true, false, true, "viaduct_connected_corner", 90, 0),
	    new MatchCondition(false, false, false, true, true, false, true, "viaduct_connected_corner", 90, 90),
	    new MatchCondition(false, false, false, true, false, true, true, "viaduct_connected_corner", 90, 180),
	    new MatchCondition(false, false, true, false, false, true, true, "viaduct_connected_corner", 90, 270),
	    new MatchCondition(false, true, true, false, false, false, false, "viaduct_connected_corner", 180, 0),
	    new MatchCondition(false, true, false, false, true, false, false, "viaduct_connected_corner", 180, 90),
	    new MatchCondition(false, true, false, true, false, false, false, "viaduct_connected_corner", 180, 180),
	    new MatchCondition(false, true, false, false, false, true, false, "viaduct_connected_corner", 180, 270),
	    new MatchCondition(true, false, true, false, false, false, false, "viaduct_connected_corner", 0, 0),
	    new MatchCondition(true, false, false, false, true, false, false, "viaduct_connected_corner", 0, 90),
	    new MatchCondition(true, false, false, true, false, false, false, "viaduct_connected_corner", 0, 180),
	    new MatchCondition(true, false, false, false, false, true, false, "viaduct_connected_corner", 0, 270),
	    new MatchCondition(false, false, true, true, true, false, false, "viaduct_connected_side", 0, -90),
	    new MatchCondition(false, false, false, true, true, true, false, "viaduct_connected_side", 0, 0),
	    new MatchCondition(false, false, true, false, true, true, false, "viaduct_connected_side", 0, 180),
	    new MatchCondition(false, false, true, true, false, true, false, "viaduct_connected_side", 0, 90),
	    new MatchCondition(false, false, true, true, false, false, false, "viaduct_connected_long", 90, 90),
	    new MatchCondition(false, false, false, false, true, true, false, "viaduct_connected_long", 90, 0),
	    new MatchCondition(true, true, false, false, false, false, false, "viaduct_connected_long", 0, 0),
	    new MatchCondition(false, false, true, false, false, false, false, "viaduct_connected", 90, 90),
	    new MatchCondition(false, false, false, true, false, false, false, "viaduct_connected", 90, -90),
	    new MatchCondition(false, false, false, false, true, false, false, "viaduct_connected", 90, 0),
	    new MatchCondition(false, false, false, false, false, true, false, "viaduct_connected", 90, 180),
	    new MatchCondition(true, false, false, false, false, false, false, "viaduct_connected", 180, 0),
	    new MatchCondition(false, true, false, false, false, false, false, "viaduct_connected", 0, 0),
	    new MatchCondition(false, false, true, true, true, true, false, "viaduct_connected_all_side", 0, 0),
	    new MatchCondition(true, true, true, true, true, true, false, "viaduct_connected_cross", 0, 0),
	    new MatchCondition(false, false, false, false, false, false, false, "viaduct", 0, 0)
	);

	
	
    private final Map<String, BakedModel> parts;
    private final BakedModel defaultModel;

    public ViaductBakedModel(Map<String, BakedModel> parts, BakedModel defaultModel) {
        this.parts = parts;
        this.defaultModel = defaultModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (state == null || !(state.getBlock() instanceof BlockViaductNew)) {
            return defaultModel.getQuads(state, side, rand, data, renderType);
        }

        String modelKey = computeModelKey(state);
        BakedModel model = parts.getOrDefault(modelKey, defaultModel);
        return model.getQuads(state, side, rand, data, renderType);
    }
    private String computeModelKey(BlockState state) {
        boolean n = state.getValue(BlockViaductNew.CONNECTED_NORTH);
        boolean s = state.getValue(BlockViaductNew.CONNECTED_SOUTH);
        boolean e = state.getValue(BlockViaductNew.CONNECTED_EAST);
        boolean w = state.getValue(BlockViaductNew.CONNECTED_WEST);
        boolean u = state.getValue(BlockViaductNew.CONNECTED_UP);
        boolean d = state.getValue(BlockViaductNew.CONNECTED_DOWN);
        boolean corner = state.getValue(BlockViaductNew.CORNER);

        for (MatchCondition cond : CONDITIONS) {
            if (cond.north == n && cond.south == s && cond.east == e && cond.west == w &&
                cond.up == u && cond.down == d && cond.corner == corner) {
                String key = cond.modelKey();
                if (cond.x != 0) key += "_x" + cond.x;
                if (cond.y != 0) key += "_y" + cond.y;
                return key;
            }
        }

        return "viaduct"; // fallback
    }

    @Override public boolean useAmbientOcclusion() { return defaultModel.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return defaultModel.isGui3d(); }
    @Override public boolean usesBlockLight() { return defaultModel.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return defaultModel.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return defaultModel.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return defaultModel.getOverrides(); }


}*/