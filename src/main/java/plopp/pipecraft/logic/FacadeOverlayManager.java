package plopp.pipecraft.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.OnlyIn;
import plopp.pipecraft.Render.FacadeOverlayRenderer;

@OnlyIn(Dist.CLIENT)
public class FacadeOverlayManager {
    private static final Map<BlockPos, FacadeData> FACADE_MAP = new HashMap<>();

    public static void addFacade(BlockPos pos, DyeColor color, boolean transparent) {
        FACADE_MAP.put(pos.immutable(), new FacadeData(color, transparent));
    }

    public static void removeFacade(BlockPos pos) {
        FACADE_MAP.remove(pos);
    }

    public static Collection<Map.Entry<BlockPos, FacadeData>> getAll() {
        return FACADE_MAP.entrySet();
    }
    
    public static boolean hasFacade(BlockPos pos) {
        return FACADE_MAP.containsKey(pos);
    }

    public record FacadeData(DyeColor color, boolean transparent) {}
    
    @OnlyIn(Dist.CLIENT)
    public record FacadeRenderData(BlockPos pos, DyeColor color, boolean transparent,
                                   boolean north, boolean south, boolean east, boolean west,
                                   boolean up, boolean down) {}
    
    public static @Nullable DyeColor getColor(BlockPos pos) {
        FacadeData data = FACADE_MAP.get(pos);
        return data != null ? data.color() : null;
    }
    
    public static void updateColor(BlockPos pos, DyeColor newColor) {
        FacadeData old = FACADE_MAP.get(pos);
        if (old != null) {
            FACADE_MAP.put(pos.immutable(), new FacadeData(newColor, old.transparent()));
            FacadeOverlayRenderer.markForReRender(pos);
        }
    }
    
}
