package com.jaquadro.minecraft.storagedrawers.core;

import com.jaquadro.minecraft.storagedrawers.ModConstants;
import com.jaquadro.minecraft.storagedrawers.core.recipe.AddUpgradeRecipe;
import com.jaquadro.minecraft.storagedrawers.core.recipe.KeyringRecipe;
import com.jaquadro.minecraft.storagedrawers.core.recipe.RemoteGroupUpgradeRecipe;
import com.jaquadro.minecraft.storagedrawers.core.recipe.UpgradeDetachedDrawerRecipe;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes
{
    private static final ChameleonRegistry<RecipeSerializer<?>> RECIPES = ChameleonServices.REGISTRY.create(BuiltInRegistries.RECIPE_SERIALIZER, ModConstants.MOD_ID);

    public static final RegistryEntry<RecipeSerializer<AddUpgradeRecipe>> UPGRADE_RECIPE_SERIALIZER = RECIPES.register("add_upgrade", () -> new CustomRecipe.Serializer(AddUpgradeRecipe::new));
    public static final RegistryEntry<RecipeSerializer<KeyringRecipe>> KEYRING_RECIPE_SERIALIZER = RECIPES.register("keyring", () -> new CustomRecipe.Serializer(KeyringRecipe::new));
    public static final RegistryEntry<RecipeSerializer<RemoteGroupUpgradeRecipe>> REMOTE_GROUP_UPGRADE_SERIALIZER = RECIPES.register("remote_group_upgrade", () -> new CustomRecipe.Serializer<>(RemoteGroupUpgradeRecipe::new));
    public static final RegistryEntry<RecipeSerializer<UpgradeDetachedDrawerRecipe>> DETACHED_UPGRADE_RECIPE_SERIALIZER = RECIPES.register("add_detached_upgrade", () -> new CustomRecipe.Serializer(UpgradeDetachedDrawerRecipe::new));

    public static void init (ChameleonInit.InitContext context) {
        RECIPES.init(context);
    }
}
