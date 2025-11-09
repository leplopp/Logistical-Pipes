package plopp.pipecraft.Blocks.Pipes.Viaduct.Item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, "pipecraft");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> COLOR =
        COMPONENTS.register("color", () ->
            DataComponentType.<DyeColor>builder()
                .persistent(DyeColor.CODEC)
                .networkSynchronized(DyeColor.STREAM_CODEC)
                .build()
        );

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}