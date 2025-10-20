package plopp.pipecraft.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.gui.pipes.PipeExtractMenu;
import plopp.pipecraft.gui.teleporter.ViaductTeleporterMenu;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerIDMenu;
import plopp.pipecraft.gui.viaductlinker.ViaductLinkerMenu;

public class MenuTypeRegister {
	
	public static final DeferredRegister<MenuType<?>> MENUS =
		    DeferredRegister.create(Registries.MENU, PipeCraftIndex.MODID);

	 public static final DeferredHolder<MenuType<?>, MenuType<ViaductLinkerMenu>> VIADUCT_LINKER =
		        registerMenuType("viaduct_linker", ViaductLinkerMenu::new);

	 public static final DeferredHolder<MenuType<?>, MenuType<ViaductLinkerIDMenu>> VIADUCT_LINKER_ID =
		        registerMenuType("viaduct_linker_id", ViaductLinkerIDMenu::new);
	 
	 public static final DeferredHolder<MenuType<?>, MenuType<ViaductTeleporterMenu>> VIADUCT_TELEPORTER =
			    registerMenuType("viaduct_teleporter", ViaductTeleporterMenu::new);
	 
	 public static final DeferredHolder<MenuType<?>, MenuType<PipeExtractMenu>> EXTRACT_PIPE =
			    registerMenuType("pipe_extract", PipeExtractMenu::new);
	 
		    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>>
		        registerMenuType(String name, IContainerFactory<T> factory) {
		      return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
		    }

		    public static void register(IEventBus bus) {
		        MENUS.register(bus);
		    }
		    
}
		
