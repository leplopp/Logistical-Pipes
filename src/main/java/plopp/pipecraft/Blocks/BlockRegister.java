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
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Pipes.BlockPipe;
import plopp.pipecraft.Blocks.Pipes.BlockPipeExtract;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductTeleporter;

public class BlockRegister {
	
	public static final DeferredRegister<Block> BLOCKS =DeferredRegister.createBlocks(PipeCraftIndex.MODID);
    public static final DeferredRegister.Items ITEMS =DeferredRegister.createItems(PipeCraftIndex.MODID);
    
    public static final DeferredBlock<Block> VIADUCTLINKER = registerBlock("viaduct_linker",
            () -> new BlockViaductLinker(BlockBehaviour.Properties.of()
                    .strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));

    public static final DeferredBlock<Block> VIADUCT = registerBlock("viaduct",
            () -> new BlockViaduct(BlockBehaviour.Properties.of()
            		.strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> VIADUCTDETECTOR = registerBlock("viaduct_detector",
            () -> new BlockViaductDetector(BlockBehaviour.Properties.of()
            		.strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> VIADUCTSPEED = registerBlock("viaduct_speed",
            () -> new BlockViaductSpeed(BlockBehaviour.Properties.of()
            		.strength(1f).explosionResistance(1.5f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> VIADUCTTELEPORTER = registerBlock("viaduct_teleporter",
            () -> new BlockViaductTeleporter(BlockBehaviour.Properties.of()
            		.strength(1f).explosionResistance(1.5f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> BLOCKPIPE = registerBlock("pipe",
            () -> new BlockPipe(BlockBehaviour.Properties.of()
                    .strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> BLOCKPIPEEXTRACT = registerBlock("pipeextract",
            () -> new BlockPipeExtract(BlockBehaviour.Properties.of()
                    .strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS), PipeConfig.defaultConfig()));
    
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
	                ViaductBlockRegistry.registerViaductBlock(VIADUCTDETECTOR.get());
	                ViaductBlockRegistry.registerViaductBlock(VIADUCTSPEED.get());
	                ViaductBlockRegistry.registerViaductBlock(VIADUCTTELEPORTER.get());
	            });
	        });
	    }
}