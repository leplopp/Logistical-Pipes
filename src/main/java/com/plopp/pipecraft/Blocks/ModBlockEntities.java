package com.plopp.pipecraft.Blocks;

import com.plopp.pipecraft.PipeCraftIndex;
import com.plopp.pipecraft.Blocks.Viaduct.ViaductBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PipeCraftIndex.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ViaductBlockEntity>> VIADUCT =
            BLOCK_ENTITIES.register("viaduct",
              () -> BlockEntityType.Builder.of(ViaductBlockEntity::new, BlockRegister.VIADUCT.get()).build(null));
   
    

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
