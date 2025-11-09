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

public class ColoredViaductBoosterRecipe  extends CustomRecipe {

    public ColoredViaductBoosterRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static final String[] PATTERN = { "CGC", "LVL", "CRC" };

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() < 3 || input.height() < 3) return false;

        for (int y = 0; y < 3; y++) {
            String row = PATTERN[y];
            for (int x = 0; x < 3; x++) {
                char key = row.charAt(x);
                ItemStack stack = input.getItem(x + y * input.width());
                if (key == 'c') {
                    if (stack.getItem() != Items.BLACK_CONCRETE) return false;
                }
                else if (key == 'L') {
                    if (stack.getItem() != Items.LIME_DYE) return false;
                }
                else if (key == 'G') {
                    if (stack.getItem() != Items.GOLD_INGOT) return false;
                }
                else if (key == 'R') {
                    if (stack.getItem() != Items.REDSTONE) return false;
                } else if (key == 'V') {
                    Item item = stack.getItem();
                    if (!(item instanceof DyedViaductItem) && item != BlockRegister.VIADUCT.get().asItem()) {
                        return false; 
                    }
                }
            }
        }
        return true;
    }
    
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(BlockRegister.VIADUCTSPEED.get());

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x + y * input.width());
                Item item = stack.getItem();
                if (item instanceof DyedViaductItem) {
                    DyeColor color = DyedViaductItem.getColor(stack);
                    DyedViaductItem.setColor(result, color); 
                    return result; 
                }
            }
        }

        DyedViaductItem.setColor(result, DyeColor.WHITE);
        return result;
    }
 
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.COLORED_VIADUCT_SPEED.get();
    }
}