package plopp.pipecraft.Recipes;

import org.jetbrains.annotations.NotNull;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;

public class DyedViaductRecipe extends CustomRecipe {

    public DyedViaductRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasViaduct = false;
        boolean hasDye = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof DyedViaductItem) {
                hasViaduct = true;
            } else if (stack.getItem() instanceof DyeItem) {
                hasDye = true;
            } else {
                return false; 
            }
        }
        return hasViaduct && hasDye;
    }
    
    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack viaductStack = ItemStack.EMPTY;
        DyeColor dye = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof DyedViaductItem && viaductStack.isEmpty()) {
                viaductStack = new ItemStack(stack.getItem(), 1);
            } else if (stack.getItem() instanceof DyeItem dyeItem && dye == null) {
                dye = dyeItem.getDyeColor();
            }
        }

        if (!viaductStack.isEmpty() && dye != null) {
            DyedViaductItem.setColor(viaductStack, dye);
            return viaductStack;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.size(), ItemStack.EMPTY);

        boolean viaductUsed = false;
        boolean dyeUsed = false;

        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);

            if (stack.isEmpty()) continue;

            if (!viaductUsed && stack.getItem() instanceof DyedViaductItem) {

                if (stack.getCount() > 1) {
                    remaining.set(i, stack.split(1)); 
                }
                viaductUsed = true;
            } else if (!dyeUsed && stack.getItem() instanceof DyeItem) {
                if (stack.getCount() > 1) {
                    remaining.set(i, stack.split(1)); 
                }
                dyeUsed = true;
            } else {
                remaining.set(i, stack); 
            }
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYED_VIADUCT.get();
    }
}