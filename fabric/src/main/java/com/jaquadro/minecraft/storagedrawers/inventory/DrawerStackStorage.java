package com.jaquadro.minecraft.storagedrawers.inventory;

import java.util.Objects;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public class DrawerStackStorage extends SingleStackStorage
{
    DrawerStorageImpl storage;
    int slot;
    ItemStack lastReleasedSnapshot = null;

    DrawerStackStorage (DrawerStorageImpl storage, int slot) {
        this.storage = storage;
        this.slot = slot;
    }

    void updateSlot (int slot) {
        this.slot = slot;
    }

    @Override
    protected ItemStack getStack () {
        IDrawer drawer = storage.getDrawer(slot);
        return drawer.getStoredItemPrototype().copyWithCount(drawer.getStoredItemCount());
    }

    @Override
    protected void setStack (ItemStack stack) {
        if (stack.getCount() > 0)
            storage.getDrawer(slot).setStoredItem(stack, stack.getCount());
        else
            storage.getDrawer(slot).setStoredItemCount(0);
    }

    @Override
    protected int getCapacity (ItemVariant itemVariant) {
        return storage.getDrawer(slot).getMaxCapacity(itemVariant.toStack());
    }

    @Override
    public long insert (ItemVariant insertedVariant, long maxAmount, TransactionContext transaction) {
        if (!storage.getDrawer(slot).canItemBeStored(insertedVariant.toStack()))
            return 0;

        long inserted = super.insert(insertedVariant, maxAmount, transaction);
        if (inserted < maxAmount) {
            IDrawerAttributes attr = storage.group.getCapability(Capabilities.DRAWER_ATTRIBUTES);
            if (attr != null && attr.isVoid())
                inserted = maxAmount;
        }

        return inserted;
    }

    @Override
    public long extract (ItemVariant variant, long maxAmount, TransactionContext transaction) {
        if (!storage.getDrawer(slot).canItemBeExtracted(variant.toStack()))
            return 0;

        return super.extract(variant, maxAmount, transaction);
    }

    @Override
    protected void releaseSnapshot (ItemStack snapshot) {
        lastReleasedSnapshot = snapshot;
    }

    @Override
    protected void onFinalCommit () {
        ItemStack original = lastReleasedSnapshot;
        ItemStack currentStack = getStack();

        if (!original.isEmpty() && original.getItem() == currentStack.getItem()) {
            if (!Objects.equals(original.getComponentsPatch(), currentStack.getComponentsPatch())) {
                for (DataComponentType<?> type : original.getComponents().keySet())
                    original.set(type, null);

                original.applyComponents(currentStack.getComponents());
            }

            original.setCount(currentStack.getCount());
            setStack(original);
        } else
            original.setCount(0);
    }
}
