package plopp.pipecraft;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;

public class PipeCreativeModeTab {
	
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PipeCraftIndex.MODID);
	    
    public static final Supplier<CreativeModeTab> PIPECRAFT_VIADCUT_TAB =
    	    CREATIVE_MODE_TABS.register("viaduct_tab",
    	        () -> CreativeModeTab.builder()
    	            .icon(() -> new ItemStack(BlockRegister.VIADUCTLINKER))
    	            .title(Component.translatable("creativetab.logisticpipes.viaduct_blocks"))
    	            .displayItems((itemDisplayParameters, output) -> {
    	               
    	              Item dyedItemViaduct = BlockRegister.DYED_VIADUCT.get();
    	                for (DyeColor color : DyeColor.values()) {
    	                    ItemStack stack = new ItemStack(dyedItemViaduct);
    	                    DyedViaductItem.setColor(stack, color);
    	                    output.accept(stack);
    	                }
    	                
    	                Item dyedItemViaductConnector = BlockRegister.DYED_VIADUCT_CONNECTOR.get();
    	                for (DyeColor color : DyeColor.values()) {
    	                    ItemStack stack = new ItemStack(dyedItemViaductConnector);
    	                    DyedViaductItem.setColor(stack, color);
    	                    output.accept(stack);
    	                }
    	                
    	                Item dyedItemViaductDetector = BlockRegister.DYED_VIADUCT_DETECTOR.get();
    	                for (DyeColor color : DyeColor.values()) {
    	                    ItemStack stack = new ItemStack(dyedItemViaductDetector);
    	                    DyedViaductItem.setColor(stack, color);
    	                    output.accept(stack);
    	                }
    	                
    	                Item dyedItemViaductSpeed = BlockRegister.DYED_VIADUCT_SPEED.get();
    	                for (DyeColor color : DyeColor.values()) {
    	                    ItemStack stack = new ItemStack(dyedItemViaductSpeed);
    	                    DyedViaductItem.setColor(stack, color);
    	                    output.accept(stack);
    	                }
    	                output.accept(BlockRegister.VIADUCTSPEED.get().asItem());	                
    	                if (Config.isViaductTeleporterEnabled()) {
    	                    output.accept(BlockRegister.VIADUCTTELEPORTER.get().asItem());
    	                }
    	                if (Config.isViaductFacadeEnabled()) {
    	                    output.accept(BlockRegister.VIADUCTFACADE.get().asItem());
    	                }
    	                if (Config.isBlockPipeEnabled()) {
    	                    output.accept(BlockRegister.BLOCKPIPE.get().asItem());
    	                }
    	                if (Config.isBlockPipeExtractEnabled()) {
    	                    output.accept(BlockRegister.BLOCKPIPEEXTRACT.get().asItem());
    	                }
    	            })
    	            .build()
    	    );
    
	    public static void register(IEventBus eventBus) {
	        CREATIVE_MODE_TABS.register(eventBus);
	    }
}