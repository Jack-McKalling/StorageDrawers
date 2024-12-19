package com.jaquadro.minecraft.storagedrawers.util;

import com.jaquadro.minecraft.storagedrawers.ModServices;
import com.jaquadro.minecraft.storagedrawers.config.CompTierRegistry;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CompactingHelper
{
    private static final InventoryLookup lookup1 = new InventoryLookup(1, 1);
    private static final InventoryLookup lookup2 = new InventoryLookup(2, 2);
    private static final InventoryLookup lookup3 = new InventoryLookup(3, 3);

    private final Level world;

    public static class Result
    {
        @NotNull
        private final ItemStack stack;
        private final int size;

        public Result (@NotNull ItemStack stack, int size) {
            this.stack = stack;
            this.size = size;
        }

        @NotNull
        public ItemStack getStack () {
            return stack;
        }

        public int getSize () {
            return size;
        }
    }

    public CompactingHelper (Level world) {
        this.world = world;
    }

    @NotNull
    public Result findHigherTier (@NotNull ItemStack stack) {
        boolean debugTrace = ModCommonConfig.INSTANCE.GENERAL.debugTrace.get();
        if (!world.isClientSide && debugTrace)
            ModServices.log.info("Finding ascending candidates for " + stack.toString());

        CompTierRegistry.Record record = CompTierRegistry.INSTANCE.findHigherTier(stack);
        if (record != null) {
            if (!world.isClientSide && debugTrace)
                ModServices.log.info("Found " + record.upper.toString() + " in registry with conv=" + record.convRate);

            return new Result(record.upper, record.convRate);
        }

        List<ItemStack> candidates = new ArrayList<>();

        int lookupSize = setupLookup(lookup3, stack);
        List<ItemStack> fwdCandidates = findAllMatchingRecipes(lookup3);

        if (fwdCandidates.size() == 0) {
            lookupSize = setupLookup(lookup2, stack);
            fwdCandidates = findAllMatchingRecipes(lookup2);
        }

        if (fwdCandidates.size() > 0) {
            for (ItemStack match : fwdCandidates) {
                setupLookup(lookup1, match);
                List<ItemStack> backCandidates = findAllMatchingRecipes(lookup1);

                for (ItemStack comp : backCandidates) {
                    if (comp.getCount() != lookupSize)
                        continue;

                    // TODO: ItemStackMatcher.areItemsEqual(comp, stack, false)
                    if (!ItemStackMatcher.areItemsEqual(comp, stack))
                        continue;

                    candidates.add(match);
                    if (!world.isClientSide && debugTrace)
                        ModServices.log.info("Found ascending candidate for " + stack.toString() + ": " + match.toString() + " size=" + lookupSize + ", inverse=" + comp.toString());

                    break;
                }
            }
        }

        List<Item> candidateItems = candidates.stream().map(ItemStack::getItem).toList();
        ItemStack modMatch = findMatchingModCandidate(stack, candidateItems);
        if (!modMatch.isEmpty())
            return new Result(modMatch, lookupSize);

        if (candidates.size() > 0)
            return new Result(candidates.get(0), lookupSize);

        if (!world.isClientSide && debugTrace)
            ModServices.log.info("No candidates found");

        return new Result(ItemStack.EMPTY, 0);
    }

    @NotNull
    public Result findLowerTier (@NotNull ItemStack stack) {
        boolean debugTrace = ModCommonConfig.INSTANCE.GENERAL.debugTrace.get();
        if (!world.isClientSide && debugTrace)
            ModServices.log.info("Finding descending candidates for " + stack.toString());

        CompTierRegistry.Record record = CompTierRegistry.INSTANCE.findLowerTier(stack);
        if (record != null) {
            if (!world.isClientSide && debugTrace)
                ModServices.log.info("Found " + record.lower.toString() + " in registry with conv=" + record.convRate);

            return new Result(record.lower, record.convRate);
        }

        List<ItemStack> candidates = new ArrayList<>();
        Map<ItemStack, Integer> candidatesRate = new HashMap<>();

        if (world instanceof ServerLevel serverWorld) {
            for (var recipe : serverWorld.recipeAccess().recipes.byType(RecipeType.CRAFTING)) {
                ItemStack output;
                List<Optional<Ingredient>> ingredients;
                if (recipe.value() instanceof ShapedRecipe shaped) {
                    output = shaped.result;
                    ingredients = shaped.pattern.ingredients();
                } else
                    continue;

                // TODO: ItemStackOreMatcher.areItemsEqual(stack, output, true)
                if (!ItemStackMatcher.areItemsEqual(stack, output))
                    continue;

                @NotNull ItemStack match = tryMatch(stack, ingredients);
                if (!match.isEmpty()) {
                    int lookupSize = setupLookup(lookup1, output);
                    List<ItemStack> compMatches = findAllMatchingRecipes(lookup1);
                    for (ItemStack comp : compMatches) {
                        int recipeSize = ingredients.size();
                        // TODO: ItemStackOreMatcher.areItemsEqual(match, comp, true)
                        if (ItemStackMatcher.areItemsEqual(match, comp) && comp.getCount() == recipeSize) {
                            candidates.add(match);
                            candidatesRate.put(match, recipeSize);

                            if (!world.isClientSide && debugTrace)
                                ModServices.log.info("Found descending candidate for " + stack.toString() + ": " + match.toString() + " size=" + recipeSize + ", inverse=" + comp.toString());
                        } else if (!world.isClientSide && debugTrace)
                            ModServices.log.info("Back-check failed for " + match.toString() + " size=" + lookupSize + ", inverse=" + comp.toString());
                    }
                }
            }
        }

        List<Item> candidateItems = candidates.stream().map(ItemStack::getItem).toList();
        ItemStack modMatch = findMatchingModCandidate(stack, candidateItems);
        if (!modMatch.isEmpty())
            return new Result(modMatch, candidatesRate.get(modMatch));

        if (candidates.size() > 0) {
            ItemStack match = candidates.get(0);
            return new Result(match, candidatesRate.get(match));
        }

        if (!world.isClientSide && debugTrace)
            ModServices.log.info("No candidates found");

        return new Result(ItemStack.EMPTY, 0);
    }

    private List<ItemStack> findAllMatchingRecipes (CraftingContainer crafting) {
        List<ItemStack> candidates = new ArrayList<>();

        CraftingInput input = crafting.asCraftInput();
        if (world instanceof ServerLevel serverWorld) {
            for (RecipeHolder<CraftingRecipe> recipe : serverWorld.recipeAccess().recipes.getRecipesFor(RecipeType.CRAFTING, input, world).toList()) {
                if (recipe.value().matches(input, world)) {
                    ItemStack result = recipe.value().assemble(input, world.registryAccess());
                    if (!result.isEmpty())
                        candidates.add(result);
                }
            }
        }

        return candidates;
    }

    @NotNull
    private ItemStack findMatchingModCandidate (@NotNull ItemStack reference, List<Item> candidates) {
        ResourceLocation referenceName = BuiltInRegistries.ITEM.getKey(reference.getItem());
        if (referenceName != null) {
            for (Item candidate : candidates) {
                ResourceLocation matchName = BuiltInRegistries.ITEM.getKey(candidate);
                if (matchName != null) {
                    if (referenceName.getNamespace().equals(matchName.getPath()))
                        return new ItemStack(candidate);
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @NotNull
    private ItemStack tryMatch (@NotNull ItemStack stack, List<Optional<Ingredient>> ingredients) {
        if (ingredients.size() != 9 && ingredients.size() != 4)
            return ItemStack.EMPTY;

        if (ingredients.getFirst().isEmpty())
            return ItemStack.EMPTY;

        Ingredient refIngredient = ingredients.getFirst().get();
        List<Item> refMatchingItems = refIngredient.items().map(Holder::value).toList();
        if (refMatchingItems.isEmpty())
            return ItemStack.EMPTY;

        for (int i = 1, n = ingredients.size(); i < n; i++) {
            if (ingredients.get(i).isEmpty())
                return ItemStack.EMPTY;

            boolean match = false;
            for (var refItem : refMatchingItems) {
                List<Item> slotItems = ingredients.get(i).get().items().map(Holder::value).toList();
                for (var slotItem : slotItems) {
                    if (refItem.equals(slotItem)) {
                        match = true;
                        break;
                    }
                }
            }

            if (!match)
                return ItemStack.EMPTY;
        }

        ItemStack match = findMatchingModCandidate(stack, refMatchingItems);
        if (match.isEmpty())
            match = new ItemStack(refMatchingItems.getFirst());

        return match;
    }

    private int setupLookup (InventoryLookup inv, @NotNull ItemStack stack) {
        for (int i = 0, n = inv.getContainerSize(); i < n; i++)
            inv.setItem(i, stack);

        return inv.getContainerSize();
    }

    private static class InventoryLookup extends TransientCraftingContainer
    {
        private final NonNullList<ItemStack> items;

        public InventoryLookup (int width, int height) {
            super(null, width, height);
            items = NonNullList.withSize(width * height, ItemStack.EMPTY);
        }

        @Override
        public int getContainerSize ()
        {
            return this.items.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack itemstack : this.items) {
                if (!itemstack.isEmpty()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= this.getContainerSize() ? ItemStack.EMPTY : this.items.get(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            stack = stack.copy();
            stack.setCount(1);
            this.items.set(slot, stack);
        }

        @Override
        @NotNull
        public ItemStack removeItemNoUpdate (int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        @NotNull
        public ItemStack removeItem (int slot, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        public void clearContent() {

        }

        @Override
        public List<ItemStack> getItems() {
            return List.copyOf(this.items);
        }
    }
}
