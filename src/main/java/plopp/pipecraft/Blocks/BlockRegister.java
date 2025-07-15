package plopp.pipecraft.Blocks;

import com.google.common.base.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;

public class BlockRegister {
	
	public static final DeferredRegister<Block> BLOCKS =DeferredRegister.createBlocks(PipeCraftIndex.MODID);
    public static final DeferredRegister.Items ITEMS =DeferredRegister.createItems(PipeCraftIndex.MODID);
    
    public static final DeferredBlock<Block> VIADUCTLINKER = registerBlock("viaduct_linker",
            () -> new BlockViaductLinker(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<Block> VIADUCT = registerBlock("viaduct",
            () -> new BlockViaduct(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.GLASS)));
    
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = (DeferredBlock<T>) BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <B extends Block> void registerBlockItem(String name, DeferredBlock<B> block) {
        ITEMS.registerSimpleBlockItem(name, block, new Item.Properties());
    }

	 public static void register(IEventBus bus) {
	        BLOCKS.register(bus);
	        ITEMS.register(bus);

	        //register travel through blocks 
	        bus.addListener((FMLCommonSetupEvent event) -> {
	            event.enqueueWork(() -> {
	                ViaductBlockRegistry.registerViaductBlock(VIADUCT.get());
	                ViaductBlockRegistry.registerViaductBlock(VIADUCTLINKER.get());
	            });
	        });
	    }
}
