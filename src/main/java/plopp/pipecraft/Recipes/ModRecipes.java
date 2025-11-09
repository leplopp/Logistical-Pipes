package plopp.pipecraft.Recipes;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import plopp.pipecraft.PipeCraftIndex;

public class ModRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister
			.create(BuiltInRegistries.RECIPE_SERIALIZER, PipeCraftIndex.MODID);

	public static final Supplier<RecipeSerializer<?>> DYED_VIADUCT = RECIPES.register("dyed_viaduct",
			() -> new SimpleCraftingRecipeSerializer<>(DyedViaductRecipe::new));

	public static final Supplier<RecipeSerializer<?>> DYED_VIADUCT_TO_CONNECTOR = RECIPES.register(
			"dyed_viaduct_to_connector", () -> new SimpleCraftingRecipeSerializer<>(DyedViaductToConnectorRecipe::new));

	public static final Supplier<RecipeSerializer<?>> COLORED_VIADUCT = RECIPES.register("colored_viadcut",
			() -> new SimpleCraftingRecipeSerializer<>(ColoredViaductRecipe::new));

	public static final Supplier<RecipeSerializer<?>> COLORED_VIADUCT_CONNECTOR = RECIPES.register(
			"colored_viadcut_connector",
			() -> new SimpleCraftingRecipeSerializer<>(ColoredViaductConnectorRecipe::new));

	public static final Supplier<RecipeSerializer<?>> COLORED_VIADUCT_DETECTOR = RECIPES.register(
			"colored_viadcut_detector", () -> new SimpleCraftingRecipeSerializer<>(ColoredViaductDetectorRecipe::new));

	public static final Supplier<RecipeSerializer<?>> COLORED_VIADUCT_SPEED = RECIPES.register("colored_viaduct_speed",
			() -> new SimpleCraftingRecipeSerializer<>(ColoredViaductBoosterRecipe::new));
}