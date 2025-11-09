package plopp.pipecraft.logic.Travel;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductTeleporter;
import plopp.pipecraft.logic.DimBlockPos;

public class TravelPathSplitter {

    public static List<List<DimBlockPos>> splitPath(List<DimBlockPos> fullPath, MinecraftServer server, @Nullable DimBlockPos knownTarget) {
        List<List<DimBlockPos>> phases = new ArrayList<>();
        List<DimBlockPos> phase = new ArrayList<>();

        for (DimBlockPos pos : fullPath) {
            phase.add(pos);

            ServerLevel level = server.getLevel(pos.getDimension());
            if (level != null) {
                BlockEntity be = level.getBlockEntity(pos.getPos());

                if (be instanceof BlockEntityViaductTeleporter || (knownTarget != null && knownTarget.equals(pos))) {
                    phases.add(new ArrayList<>(phase));
                    phase.clear();
                }
            }
        }

        if (!phase.isEmpty()) {
            phases.add(new ArrayList<>(phase));
        }
        return phases;
    }
}