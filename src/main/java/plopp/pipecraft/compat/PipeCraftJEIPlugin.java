package plopp.pipecraft.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import plopp.pipecraft.PipeCraftIndex;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;

@JeiPlugin
public class PipeCraftJEIPlugin implements IModPlugin {
	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("yourmodid", "jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

		DyeColor[] glassColors = DyeColor.values();

		for (DyeColor color : glassColors) {
			ItemStack result = new ItemStack(BlockRegister.VIADUCT.get(), 4);
			DyedViaductItem.setColor(result, color);

			Map<Character, Ingredient> key = new HashMap<>();
			key.put('I', Ingredient.of(Items.IRON_INGOT));
			key.put('G', Ingredient.of(getGlassForColor(color)));

			String[] pattern = { "IGI", "GIG", "IGI" };

			ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);

			ShapedRecipe recipe = new ShapedRecipe("viaduct_colored" + color.getName(), CraftingBookCategory.BUILDING,
					shapedPattern, result);

			recipes.add(new RecipeHolder<>(
					ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "viaduct_colored" + color.getName()),
					recipe));
		}

		for (DyeColor color : glassColors) {
			ItemStack result = new ItemStack(BlockRegister.VIADUCTLINKER.get(), 4);
			DyedViaductItem.setColor(result, color);

			Map<Character, Ingredient> key = new HashMap<>();
			key.put('I', Ingredient.of(Items.IRON_INGOT));
			key.put('A', Ingredient.of(Items.AIR));
			key.put('G', Ingredient.of(getGlassForColor(color)));

			String[] pattern = { "IGI", "GAG", "IGI" };

			ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);

			ShapedRecipe recipe = new ShapedRecipe("colored_viadcut_connector" + color.getName(),
					CraftingBookCategory.BUILDING, shapedPattern, result);

			recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID,
					"colored_viadcut_connector" + color.getName()), recipe));
		}

		for (DyeColor color : glassColors) {
			ItemStack result = new ItemStack(BlockRegister.VIADUCTDETECTOR.get());
			DyedViaductItem.setColor(result, color);

			ItemStack inputViaduct = new ItemStack(BlockRegister.VIADUCT.get());
			DyedViaductItem.setColor(inputViaduct, color);

			Map<Character, Ingredient> key = new HashMap<>();
			key.put('R', Ingredient.of(Items.REDSTONE));
			key.put('A', Ingredient.of(Items.AIR));
			key.put('V', Ingredient.of(inputViaduct));

			String[] pattern = { "ARA", "RVR", "ARA" };

			ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);

			ShapedRecipe recipe = new ShapedRecipe("colored_viadcut_detector" + color.getName(),
					CraftingBookCategory.BUILDING, shapedPattern, result);

			recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID,
					"colored_viadcut_detector" + color.getName()), recipe));
		}

		for (DyeColor color : glassColors) {
			ItemStack result = new ItemStack(BlockRegister.VIADUCTSPEED.get());
			DyedViaductItem.setColor(result, color);

			ItemStack inputViaduct = new ItemStack(BlockRegister.VIADUCT.get());
			DyedViaductItem.setColor(inputViaduct, color);

			Map<Character, Ingredient> key = new HashMap<>();
			key.put('R', Ingredient.of(Items.REDSTONE));
			key.put('C', Ingredient.of(Items.BLACK_CONCRETE));
			key.put('G', Ingredient.of(Items.GOLD_INGOT));
			key.put('L', Ingredient.of(Items.LIME_DYE));
			key.put('V', Ingredient.of(inputViaduct));

			String[] pattern = { "CGC", "LVL", "CRC" };

			ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);

			ShapedRecipe recipe = new ShapedRecipe("colored_viaduct_speed" + color.getName(),
					CraftingBookCategory.BUILDING, shapedPattern, result);

			recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID,
					"colored_viaduct_speed" + color.getName()), recipe));
		}

		List<Block> viaductVariants = List.of(
			    BlockRegister.VIADUCT.get(),
			    BlockRegister.VIADUCTDETECTOR.get(),
			    BlockRegister.VIADUCTSPEED.get(),
			    BlockRegister.VIADUCTLINKER.get()
			);

			DyeColor[] colors = DyeColor.values();

			for (Block viaduct : viaductVariants) {
			    ResourceLocation viaductId = BuiltInRegistries.BLOCK.getKey(viaduct);

			    for (DyeColor color : colors) {
			        ItemStack result = new ItemStack(viaduct);
			        DyedViaductItem.setColor(result, color);

			        Map<Character, Ingredient> key = new HashMap<>();
			        key.put('I', Ingredient.of(getDyeItem(color)));

			        List<ItemStack> viaductColorVariants = new ArrayList<>();
			        for (DyeColor variantColor : DyeColor.values()) {
			            ItemStack dyed = new ItemStack(viaduct);
			            DyedViaductItem.setColor(dyed, variantColor);
			            viaductColorVariants.add(dyed);
			        }
			        key.put('V', Ingredient.of(viaductColorVariants.toArray(ItemStack[]::new)));

			        String[] pattern = { "IV" };

			        ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);

			        ShapedRecipe recipe = new ShapedRecipe(
			            "dyed_" + viaductId.getPath() + "_" + color.getName(),
			            CraftingBookCategory.BUILDING,
			            shapedPattern,
			            result
			        );

			        recipes.add(new RecipeHolder<>(
			            ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID,
			                "dyed_" + viaductId.getPath() + "_" + color.getName()),
			            recipe
			        ));
			    }
			}

		for (DyeColor color : glassColors) {
			ItemStack result = new ItemStack(BlockRegister.VIADUCTLINKER.get(), 1);
			DyedViaductItem.setColor(result, color);

			ItemStack input = new ItemStack(BlockRegister.VIADUCT.get());
			DyedViaductItem.setColor(input, color);

			Map<Character, Ingredient> key = new HashMap<>();
			key.put('V', Ingredient.of(input));

			String[] pattern = { "V" };

			ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);

			ShapedRecipe recipe = new ShapedRecipe("viaduct_to_linker_" + color.getName(),
					CraftingBookCategory.BUILDING, shapedPattern, result);

			recipes.add(new RecipeHolder<>(
					ResourceLocation.fromNamespaceAndPath(PipeCraftIndex.MODID, "viaduct_to_linker_" + color.getName()),
					recipe));
		}

		registration.addRecipes(RecipeTypes.CRAFTING, recipes);
	}

	private Item getGlassForColor(DyeColor color) {
		return switch (color) {
		case WHITE -> Items.WHITE_STAINED_GLASS_PANE;
		case ORANGE -> Items.ORANGE_STAINED_GLASS_PANE;
		case MAGENTA -> Items.MAGENTA_STAINED_GLASS_PANE;
		case LIGHT_BLUE -> Items.LIGHT_BLUE_STAINED_GLASS_PANE;
		case YELLOW -> Items.YELLOW_STAINED_GLASS_PANE;
		case LIME -> Items.LIME_STAINED_GLASS_PANE;
		case PINK -> Items.PINK_STAINED_GLASS_PANE;
		case GRAY -> Items.GRAY_STAINED_GLASS_PANE;
		case LIGHT_GRAY -> Items.LIGHT_GRAY_STAINED_GLASS_PANE;
		case CYAN -> Items.CYAN_STAINED_GLASS_PANE;
		case PURPLE -> Items.PURPLE_STAINED_GLASS_PANE;
		case BLUE -> Items.BLUE_STAINED_GLASS_PANE;
		case BROWN -> Items.BROWN_STAINED_GLASS_PANE;
		case GREEN -> Items.GREEN_STAINED_GLASS_PANE;
		case RED -> Items.RED_STAINED_GLASS_PANE;
		case BLACK -> Items.BLACK_STAINED_GLASS_PANE;
		};
	}

	private Item getDyeItem(DyeColor color) {
		return switch (color) {
		case WHITE -> Items.WHITE_DYE;
		case ORANGE -> Items.ORANGE_DYE;
		case MAGENTA -> Items.MAGENTA_DYE;
		case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
		case YELLOW -> Items.YELLOW_DYE;
		case LIME -> Items.LIME_DYE;
		case PINK -> Items.PINK_DYE;
		case GRAY -> Items.GRAY_DYE;
		case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
		case CYAN -> Items.CYAN_DYE;
		case PURPLE -> Items.PURPLE_DYE;
		case BLUE -> Items.BLUE_DYE;
		case BROWN -> Items.BROWN_DYE;
		case GREEN -> Items.GREEN_DYE;
		case RED -> Items.RED_DYE;
		case BLACK -> Items.BLACK_DYE;
		};
	}
}