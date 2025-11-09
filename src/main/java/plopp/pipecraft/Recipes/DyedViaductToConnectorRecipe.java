package plopp.pipecraft.Recipes;

import org.jetbrains.annotations.NotNull;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import plopp.pipecraft.Blocks.BlockRegister;
import plopp.pipecraft.Blocks.Pipes.Viaduct.Item.DyedViaductItem;

public class DyedViaductToConnectorRecipe extends CustomRecipe {

    public DyedViaductToConnectorRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasDyedViaduct = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            if (item instanceof DyedViaductItem) {
                if (item == BlockRegister.DYED_VIADUCT_SPEED.get() || item == BlockRegister.DYED_VIADUCT_DETECTOR.get()  || item == BlockRegister.DYED_VIADUCT_CONNECTOR.get()) {
                    return false; 
                }

                if (hasDyedViaduct) return false;
                hasDyedViaduct = true;
            } else {
                return false; 
            }
        }

        return hasDyedViaduct;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof DyedViaductItem) {
                ItemStack connector = new ItemStack(BlockRegister.VIADUCTLINKER.get(), 1);

                DyeColor color = DyedViaductItem.getColor(stack);
                if (color != null) {
                    DyedViaductItem.setColor(connector, color); 
                }

                return connector;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.size(), ItemStack.EMPTY);

        boolean used = false;
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!used && stack.getItem() instanceof DyedViaductItem) {
                if (stack.getCount() > 1) remaining.set(i, stack.split(1));
                used = true;
            } else if (!stack.isEmpty()) {
                remaining.set(i, stack);
            }
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1; 
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYED_VIADUCT_TO_CONNECTOR.get();
    }
}