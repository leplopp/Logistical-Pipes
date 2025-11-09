package plopp.pipecraft.Blocks;

import net.minecraft.world.item.Item;
import com.google.common.base.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import plopp.pipecraft.PipeConfig;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.Facade.BlockViaductFacade;
import plopp.pipecraft.Blocks.Pipes.ItemPipes.BlockPipe;
import plopp.pipecraft.Blocks.Pipes.ItemPipes.BlockPipeExtract;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductTeleporter;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;

public class BlockRegister {
	
	public static final DeferredRegister<Block> BLOCKS =DeferredRegister.createBlocks(PipeCraftIndex.MODID);
    public static final DeferredRegister.Items ITEMS =DeferredRegister.createItems(PipeCraftIndex.MODID);
    
    public static final DeferredBlock<Block> VIADUCTLINKER = registerBlock("viaduct_linker",
            () -> new BlockViaductLinker(BlockBehaviour.Properties.of()
                    .strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    public static final DeferredItem<DyedViaductItem> DYED_VIADUCT_CONNECTOR  =
    	    ITEMS.register("viaduct_linker", () -> new DyedViaductItem(VIADUCTLINKER.get(), VIADUCTLINKER.getRegisteredName(), new Item.Properties()));

    public static final DeferredBlock<Block> VIADUCT = registerBlock("viaduct",
            () -> new BlockViaduct(BlockBehaviour.Properties.of()
            		.strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    public static final DeferredItem<DyedViaductItem> DYED_VIADUCT =
    	    ITEMS.register("viaduct", () -> new DyedViaductItem(VIADUCT.get(), VIADUCT.getRegisteredName(), new Item.Properties()));
    
    public static final DeferredBlock<Block> VIADUCTFACADE = registerBlock("viaduct_facade",
            () -> new BlockViaductFacade(BlockBehaviour.Properties.of()
            		.strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> VIADUCTDETECTOR = registerBlock("viaduct_detector",
            () -> new BlockViaductDetector(BlockBehaviour.Properties.of()
            		.strength(1f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    public static final DeferredItem<DyedViaductItem> DYED_VIADUCT_DETECTOR =
    	    ITEMS.register("viaduct_detector", () -> new DyedViaductItem(VIADUCTDETECTOR.get(), VIADUCTDETECTOR.getRegisteredName(), new Item.Properties()));
    
    public static final DeferredBlock<Block> VIADUCTSPEED = registerBlock("viaduct_speed",
            () -> new BlockViaductSpeed(BlockBehaviour.Properties.of()
            		.strength(1f).explosionResistance(1.5f).sound(SoundType.GLASS)));
    public static final DeferredItem<DyedViaductItem> DYED_VIADUCT_SPEED =
    	    ITEMS.register("viaduct_speed", () -> new DyedViaductItem(VIADUCTSPEED.get(), VIADUCTSPEED.getRegisteredName(), new Item.Properties()));
    
    public static final DeferredBlock<Block> VIADUCTTELEPORTER = registerBlock("viaduct_teleporter",
            () -> new BlockViaductTeleporter(BlockBehaviour.Properties.of()
            		.strength(1f).explosionResistance(1.5f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> BLOCKPIPE = registerBlock("pipe",
            () -> new BlockPipe(BlockBehaviour.Properties.of()
                    .strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS)));
    
    public static final DeferredBlock<Block> BLOCKPIPEEXTRACT = registerBlock("pipe_extract",
            () -> new BlockPipeExtract(BlockBehaviour.Properties.of()
                    .strength(0.5f).explosionResistance(1.0f).sound(SoundType.GLASS), PipeConfig.defaultConfig()));
    
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = (DeferredBlock<T>) BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <B extends Block> void registerBlockItem(String name, DeferredBlock<B> block) {
    	 if (name.equals("viaduct") || name.equals("viaduct_linker") || name.equals("viaduct_speed") || name.equals("viaduct_detector")) return;
        if (name.startsWith("viaduct") || name.startsWith("pipe")) {
            ITEMS.register(name, () -> new DyedViaductItem(block.get(), name, new Item.Properties()));
        } else {
            ITEMS.registerSimpleBlockItem(name, block, new Item.Properties());
        }
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