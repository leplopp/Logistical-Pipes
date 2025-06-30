package com.plopp.pipecraft.Blocks;

import com.plopp.pipecraft.PipeCraftIndex;
import com.plopp.pipecraft.Blocks.Pipes.Viaduct.BlockEntityViaductLinker;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockEntityRegister {
	
	 public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,  PipeCraftIndex.MODID);

		    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityViaductLinker>> VIADUCT_LINKER =
		        BLOCK_ENTITIES.register("viaduct_linker", () ->
		            BlockEntityType.Builder.of(BlockEntityViaductLinker::new, BlockRegister.VIADUCTLINKER.get()).build(null)
		        );

		    public static void register(IEventBus bus) {
		        BLOCK_ENTITIES.register(bus);
		    }
		
}
