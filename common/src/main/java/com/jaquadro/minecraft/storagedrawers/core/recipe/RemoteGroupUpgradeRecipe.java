package com.jaquadro.minecraft.storagedrawers.core.recipe;

import com.jaquadro.minecraft.storagedrawers.core.ModDataComponents;
import com.jaquadro.minecraft.storagedrawers.core.ModItems;
import com.jaquadro.minecraft.storagedrawers.core.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.util.Map;
import java.util.Optional;

public class RemoteGroupUpgradeRecipe extends ShapedRecipe
{
    public RemoteGroupUpgradeRecipe (CraftingBookCategory cat) {
        super("", cat, pattern(), new ItemStack(ModItems.REMOTE_GROUP_UPGRADE_BOUND.get()));
    }

    private static ShapedRecipePattern pattern () {
        return ShapedRecipePattern.of(Map.of(
                'X', Ingredient.of(Items.ENDER_PEARL),
                '#', Ingredient.of(ModItems.REMOTE_UPGRADE_BOUND.get())),
            "X#X");
    }

    @Override
    public ItemStack assemble (CraftingInput inv, HolderLookup.Provider registries) {
        ItemStack center = inv.getItem(1);
        if (center == ItemStack.EMPTY)
            center = inv.getItem(4);
        if (center == ItemStack.EMPTY)
            center = inv.getItem(7);

        if (center.isEmpty() || center.getItem() != ModItems.REMOTE_UPGRADE_BOUND.get())
            return ItemStack.EMPTY;

        ItemStack result = new ItemStack(ModItems.REMOTE_GROUP_UPGRADE_BOUND.get());
        result.set(ModDataComponents.CONTROLLER_BINDING.get(), center.get(ModDataComponents.CONTROLLER_BINDING.get()));

        return result;
    }

    @Override
    public RecipeSerializer<? extends ShapedRecipe>  getSerializer () {
        return ModRecipes.REMOTE_GROUP_UPGRADE_SERIALIZER.get();
    }
}
