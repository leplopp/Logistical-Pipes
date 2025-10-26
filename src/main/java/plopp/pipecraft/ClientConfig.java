package plopp.pipecraft;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public class ClientConfig {
	public static final ModConfigSpec SPEC;
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ModConfigSpec.IntValue ITEM_RENDER_DISTANCE_CHUNKS;
	private static final ModConfigSpec.EnumValue<RenderStageOption> ITEM_RENDER_STAGE;
	private static final DoubleValue VIADUCT_START_VOLUME;
	private static final DoubleValue VIADUCT_STOP_VOLUME;
	private static final DoubleValue VIADUCT_TRAVEL_VOLUME;

	public enum RenderStageOption {
		AFTER_SOLID_BLOCKS, AFTER_CUTOUT_BLOCKS, AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS, AFTER_TRANSLUCENT_BLOCKS,
		AFTER_PARTICLES, AFTER_BLOCK_ENTITIES
	}

	static {
		BUILDER.push("Item Pipe Render Distance");
		BUILDER.comment("");		
		ITEM_RENDER_DISTANCE_CHUNKS = BUILDER
				.comment("Maximum render distance (in chunks) for travelling items. Default: 8, Max: 32")
				.defineInRange("itemRenderDistanceChunks", 16, 1, 32);

		BUILDER.pop();

		BUILDER.push("Item Pipe Render Stage");
		BUILDER.comment("");		
		ITEM_RENDER_STAGE = BUILDER.comment(
				"Render stage for travelling items. Allows testing visibility in different graphics modes. only for testing")
				.defineEnum("itemRenderStage", RenderStageOption.AFTER_SOLID_BLOCKS);
		BUILDER.pop();

		BUILDER.push("Mod Sound Volumes");
		BUILDER.comment("");		
		VIADUCT_START_VOLUME = BUILDER.comment("Volume multiplier for Viaduct start sound (0.1 - 2.0, default 1.0)")
				.defineInRange("viaductStartVolume", 1.0, 0.1, 2.0);
		BUILDER.comment("");		
		VIADUCT_STOP_VOLUME = BUILDER.comment("Volume multiplier for Viaduct stop sound (0.1 - 2.0, default 1.0)")
				.defineInRange("viaductStopVolume", 1.0, 0.1, 2.0);
		BUILDER.comment("");		
		VIADUCT_TRAVEL_VOLUME = BUILDER.comment("Volume multiplier for Viaduct Travel sound (0.1 - 2.0, default 1.0)")
				.defineInRange("viaductTravelVolume", 1.0, 0.1, 2.0);

		BUILDER.pop();

		SPEC = BUILDER.build();

	}

	public static int getItemRenderDistanceBlocks() {
		return ITEM_RENDER_DISTANCE_CHUNKS.get() * 16;
	}

	public static float getViaductStartVolume() {
		return VIADUCT_START_VOLUME.get().floatValue();
	}

	public static float getViaductStopVolume() {
		return VIADUCT_STOP_VOLUME.get().floatValue();
	}

	public static float getViaductTravelVolume() {
		return VIADUCT_TRAVEL_VOLUME.get().floatValue();
	}

	public static RenderLevelStageEvent.Stage getRenderStage() {
		switch (ITEM_RENDER_STAGE.get()) {
		case AFTER_SOLID_BLOCKS:
			return RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
		case AFTER_CUTOUT_BLOCKS:
			return RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
		case AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS:
			return RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS;
		case AFTER_TRANSLUCENT_BLOCKS:
			return RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
		case AFTER_PARTICLES:
			return RenderLevelStageEvent.Stage.AFTER_PARTICLES;
		case AFTER_BLOCK_ENTITIES:
			return RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
		}
		// Fallback
		return RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
	}
}