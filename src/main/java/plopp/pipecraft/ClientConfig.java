package plopp.pipecraft;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
	public static final ModConfigSpec SPEC;
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	
	private static final ModConfigSpec.IntValue ITEM_RENDER_DISTANCE_CHUNKS;
    private static final ModConfigSpec.EnumValue<RenderStageOption> ITEM_RENDER_STAGE;
    
    public enum RenderStageOption {
        AFTER_SOLID_BLOCKS,
        AFTER_CUTOUT_BLOCKS,
        AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_PARTICLES,
        AFTER_BLOCK_ENTITIES
    }
    
	static {
		BUILDER.push("Item Pipe Render Distance");

		ITEM_RENDER_DISTANCE_CHUNKS = BUILDER
				.comment("Maximum render distance (in chunks) for travelling items. Default: 8, Max: 32")
				.defineInRange("itemRenderDistanceChunks", 16, 1, 32);

		BUILDER.pop();
		
		 BUILDER.push("Item Pipe Render Stage");
	        ITEM_RENDER_STAGE = BUILDER
	                .comment("Render stage for travelling items. Allows testing visibility in different graphics modes. only for testing")
	                .defineEnum("itemRenderStage", RenderStageOption.AFTER_SOLID_BLOCKS);
	        BUILDER.pop();


		SPEC = BUILDER.build();
	}

	public static int getItemRenderDistanceBlocks() {
		return ITEM_RENDER_DISTANCE_CHUNKS.get() * 16;
	}
	
	  public static RenderLevelStageEvent.Stage getRenderStage() {
	        switch (ITEM_RENDER_STAGE.get()) {
	            case AFTER_SOLID_BLOCKS: return RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
	            case AFTER_CUTOUT_BLOCKS: return RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
	            case AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS: return RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS;
	            case AFTER_TRANSLUCENT_BLOCKS: return RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
	            case AFTER_PARTICLES: return RenderLevelStageEvent.Stage.AFTER_PARTICLES;
	            case AFTER_BLOCK_ENTITIES: return RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
	        }
	        // Fallback
	        return RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
	    }
}

