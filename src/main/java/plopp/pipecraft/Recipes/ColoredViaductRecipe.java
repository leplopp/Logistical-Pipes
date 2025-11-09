package plopp.pipecraft.Recipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;
import org.jetbrains.annotations.NotNull;

public class ColoredViaductRecipe  extends CustomRecipe {

    public ColoredViaductRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static final String[] PATTERN = { "IGI", "GIG", "IGI" };

    @Override
    public boolean matches(CraftingInput input, Level level) {

        if (input.width() < 3 || input.height() < 3) return false;

        DyeColor glassColor = null;

        for (int y = 0; y < 3; y++) {
            String row = PATTERN[y];
            for (int x = 0; x < 3; x++) {
                char key = row.charAt(x);
                ItemStack stack = input.getItem(x + y * input.width());
                if (key == 'I') {
                    if (stack.getItem() != Items.IRON_INGOT) return false;
                } else if (key == 'G') {
                    if (!isStainedGlassPane(stack.getItem())) return false;
                    DyeColor color = getColorFromGlass(stack.getItem());
                    if (glassColor == null) glassColor = color;
                    else if (glassColor != color) return false; 
                }
            }
        }
        return true;
    }
    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        DyeColor glassColor = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isStainedGlassPane(stack.getItem())) {
                glassColor = getColorFromGlass(stack.getItem());
                break;
            }
        }

        ItemStack result = new ItemStack(BlockRegister.VIADUCT.get(), 4);
        if (glassColor != null) {
            DyedViaductItem.setColor(result, glassColor);
        }

        return result;
    }
    
    private boolean isStainedGlassPane(Item item) {
        return item == Items.WHITE_STAINED_GLASS_PANE
            || item == Items.LIGHT_GRAY_STAINED_GLASS_PANE
            || item == Items.GRAY_STAINED_GLASS_PANE
            || item == Items.BLACK_STAINED_GLASS_PANE
            || item == Items.RED_STAINED_GLASS_PANE
            || item == Items.ORANGE_STAINED_GLASS_PANE
            || item == Items.YELLOW_STAINED_GLASS_PANE
            || item == Items.LIME_STAINED_GLASS_PANE
            || item == Items.GREEN_STAINED_GLASS_PANE
            || item == Items.CYAN_STAINED_GLASS_PANE
            || item == Items.LIGHT_BLUE_STAINED_GLASS_PANE
            || item == Items.BLUE_STAINED_GLASS_PANE
            || item == Items.PURPLE_STAINED_GLASS_PANE
            || item == Items.MAGENTA_STAINED_GLASS_PANE
            || item == Items.PINK_STAINED_GLASS_PANE
            || item == Items.BROWN_STAINED_GLASS_PANE;
    }

    private DyeColor getColorFromGlass(Item item) {
        if (item == Items.WHITE_STAINED_GLASS_PANE) return DyeColor.WHITE;
        if (item == Items.LIGHT_GRAY_STAINED_GLASS_PANE) return DyeColor.LIGHT_GRAY;
        if (item == Items.GRAY_STAINED_GLASS_PANE) return DyeColor.GRAY;
        if (item == Items.BLACK_STAINED_GLASS_PANE) return DyeColor.BLACK;
        if (item == Items.RED_STAINED_GLASS_PANE) return DyeColor.RED;
        if (item == Items.ORANGE_STAINED_GLASS_PANE) return DyeColor.ORANGE;
        if (item == Items.YELLOW_STAINED_GLASS_PANE) return DyeColor.YELLOW;
        if (item == Items.LIME_STAINED_GLASS_PANE) return DyeColor.LIME;
        if (item == Items.GREEN_STAINED_GLASS_PANE) return DyeColor.GREEN;
        if (item == Items.CYAN_STAINED_GLASS_PANE) return DyeColor.CYAN;
        if (item == Items.LIGHT_BLUE_STAINED_GLASS_PANE) return DyeColor.LIGHT_BLUE;
        if (item == Items.BLUE_STAINED_GLASS_PANE) return DyeColor.BLUE;
        if (item == Items.PURPLE_STAINED_GLASS_PANE) return DyeColor.PURPLE;
        if (item == Items.MAGENTA_STAINED_GLASS_PANE) return DyeColor.MAGENTA;
        if (item == Items.PINK_STAINED_GLASS_PANE) return DyeColor.PINK;
        if (item == Items.BROWN_STAINED_GLASS_PANE) return DyeColor.BROWN;
        return DyeColor.WHITE;
    }
    
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.COLORED_VIADUCT.get();
    }
}