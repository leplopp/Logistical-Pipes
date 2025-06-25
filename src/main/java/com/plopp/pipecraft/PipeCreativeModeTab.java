package com.plopp.pipecraft;

import com.plopp.pipecraft.Blocks.BlockRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;

public class PipeCreativeModeTab {
	
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PipeCraftIndex.MODID);
	    
    public static final Supplier<CreativeModeTab> PIPECRAFT_TAB =
    	    CREATIVE_MODE_TABS.register("bismuth_blocks_tab",
    	        () -> CreativeModeTab.builder()
    	            .icon(() -> new ItemStack(BlockRegister.VIADUCT))
    	            .withTabsBefore(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "pipecraft_tab"))
    	            .title(Component.translatable("creativetab.logisticpipes.pipecraft_blocks"))
    	            .displayItems((itemDisplayParameters, output) -> {
    	                output.accept(BlockRegister.VIADUCT.get().asItem());
    	                output.accept(BlockRegister.VIADUCTADVANCED.get().asItem());
    	            })
    	            .build()
    	    );


	    public static void register(IEventBus eventBus) {
	        CREATIVE_MODE_TABS.register(eventBus);
	    }
	
}
