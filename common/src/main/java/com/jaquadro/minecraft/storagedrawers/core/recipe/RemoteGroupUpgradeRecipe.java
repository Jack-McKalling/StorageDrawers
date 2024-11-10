package com.jaquadro.minecraft.storagedrawers.core.recipe;

import com.jaquadro.minecraft.storagedrawers.core.ModItems;
import com.jaquadro.minecraft.storagedrawers.core.ModRecipes;
import com.jaquadro.minecraft.storagedrawers.item.ItemKey;
import com.jaquadro.minecraft.storagedrawers.item.ItemKeyring;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class RemoteGroupUpgradeRecipe extends ShapedRecipe
{
    public RemoteGroupUpgradeRecipe (ResourceLocation name, CraftingBookCategory cat) {
        super(name, "", cat, 3, 1, NonNullList.of(Ingredient.EMPTY,
            Ingredient.of(Items.ENDER_PEARL),
                Ingredient.of(ModItems.REMOTE_UPGRADE_BOUND.get()),
            Ingredient.of(Items.ENDER_PEARL)),
            new ItemStack(ModItems.REMOTE_GROUP_UPGRADE_BOUND.get()));
    }

    @Override
    public boolean matches (CraftingContainer p_44176_, Level p_44177_) {
        return super.matches(p_44176_, p_44177_);
    }

    @Override
    public ItemStack assemble (CraftingContainer inv, RegistryAccess registries) {
        ItemStack center = inv.getItem(1);
        if (center == ItemStack.EMPTY)
            center = inv.getItem(4);
        if (center == ItemStack.EMPTY)
            center = inv.getItem(7);

        if (center.isEmpty() || center.getItem() != ModItems.REMOTE_UPGRADE_BOUND.get())
            return ItemStack.EMPTY;

        ItemStack result = new ItemStack(ModItems.REMOTE_GROUP_UPGRADE_BOUND.get());
        result.setTag(center.getTag().copy());

        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer () {
        return ModRecipes.REMOTE_GROUP_UPGRADE_SERIALIZER.get();
    }
}
