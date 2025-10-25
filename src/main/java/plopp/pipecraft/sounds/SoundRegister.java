package plopp.pipecraft.sounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import plopp.pipecraft.PipeCraftIndex;

public class SoundRegister {
	

	public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
		    DeferredRegister.create(Registries.SOUND_EVENT, PipeCraftIndex.MODID);

	  public static final DeferredHolder<SoundEvent, SoundEvent> VIADUCT_LOOP =
	            SOUND_EVENTS.register("viaduct.loop",
	                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "viaduct.loop")));

	    public static final DeferredHolder<SoundEvent, SoundEvent> VIADUCT_STOP =
	            SOUND_EVENTS.register("viaduct.stop",
	                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "viaduct.stop")));
	    
	    public static final DeferredHolder<SoundEvent, SoundEvent> VIADUCT_START =
	            SOUND_EVENTS.register("viaduct.start",
	                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "viaduct.start")));
	
}
