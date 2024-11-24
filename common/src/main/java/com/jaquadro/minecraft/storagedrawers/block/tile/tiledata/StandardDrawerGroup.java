package com.jaquadro.minecraft.storagedrawers.block.tile.tiledata;

import com.jaquadro.minecraft.storagedrawers.api.storage.*;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities;
import com.jaquadro.minecraft.storagedrawers.inventory.ItemStackHelper;
import com.jaquadro.minecraft.storagedrawers.util.ItemStackMatcher;
import com.jaquadro.minecraft.storagedrawers.util.ItemStackTagMatcher;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public abstract class StandardDrawerGroup extends BlockEntityDataShim implements IDrawerGroup
{
    private final DrawerData[] slots;
    private final int[] order;

    public StandardDrawerGroup (int slotCount) {
        slots = new DrawerData[slotCount];
        for (int i = 0; i < slotCount; i++)
            slots[i] = createDrawer(i);

        order = new int[slotCount];
        syncSlots();
    }

    @Override
    public int getDrawerCount () {
        return slots.length;
    }

    @NotNull
    @Override
    public IDrawer getDrawer (int slot) {
        if (slot < 0 || slot >= slots.length)
            return Drawers.DISABLED;

        return slots[slot];
    }

    @Override
    public int[] getAccessibleDrawerSlots () {
        return order;
    }

    @Override
    public void read (HolderLookup.Provider provider, CompoundTag tag) {
        if (!tag.contains("Drawers"))
            return;

        ListTag itemList = tag.getList("Drawers", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            if (i < slots.length)
                slots[i].deserializeNBT(provider, itemList.getCompound(i));
        }
    }

    @Override
    public CompoundTag write (HolderLookup.Provider provider, CompoundTag tag) {
        if (slots == null)
            return tag;

        ListTag itemList = new ListTag();
        for (DrawerData slot : slots)
            itemList.add(slot.serializeNBT(provider));

        tag.put("Drawers", itemList);

        return tag;
    }

    @NotNull
    protected abstract DrawerData createDrawer (int slot);

    public void syncAttributes () {
        for (DrawerData drawer : slots)
            drawer.syncAttributes();

    }

    public void syncSlots () {
        int index = 0;
        for (int i = 0; i < slots.length; i++) {
            IDrawer drawer = getDrawer(i);
            if (!drawer.isEmpty())
                order[index++] = i;
        }

        if (index != slots.length) {
            for (int i = 0; i < slots.length; i++) {
                IDrawer drawer = getDrawer(i);
                if (drawer.isEnabled() && drawer.isEmpty())
                    order[index++] = i;
            }
        }

        if (index != slots.length) {
            for (int i = 0; i < slots.length; i++) {
                IDrawer drawer = getDrawer(i);
                if (!drawer.isEnabled())
                    order[index++] = i;
            }
        }
    }

    public static class DrawerData implements IDrawer
    {
        protected IDrawerAttributes cachedAttrs;
        protected StandardDrawerGroup group;

        @NotNull
        private ItemStack protoStack;
        private int count;
        private ItemStackMatcher matcher;
        private boolean missing;

        public DrawerData (StandardDrawerGroup group) {
            this.group = group;
            protoStack = ItemStack.EMPTY;
            matcher = ItemStackMatcher.EMPTY;
            missing = false;
        }

        protected DrawerData (DrawerData data) {
            this(data.group);
            cachedAttrs = data.cachedAttrs;

            protoStack = data.protoStack;
            count = data.count;
            matcher = data.matcher;
        }

        @NotNull
        IDrawerAttributes getAttributes() {
            if (cachedAttrs != null)
                return cachedAttrs;

            cachedAttrs = group.getCapability(Capabilities.DRAWER_ATTRIBUTES);
            if (cachedAttrs != null)
                return cachedAttrs;

            return EmptyDrawerAttributes.EMPTY;
        }

        @Override
        @NotNull
        public ItemStack getStoredItemPrototype () {
            if (isMissing())
                return ItemStack.EMPTY;

            return protoStack;
        }

        @Override
        @NotNull
        public IDrawer setStoredItem (@NotNull ItemStack itemPrototype) {
            return setStoredItem(itemPrototype, true);
        }

        protected IDrawer setStoredItem (@NotNull ItemStack itemPrototype, boolean notify) {
            if (isMissing())
                return this;

            if (ItemStackHelper.isStackEncoded(itemPrototype))
                itemPrototype = ItemStackHelper.decodeItemStackPrototype(itemPrototype);

            if (matcher.matches(itemPrototype))
                return this;

            itemPrototype = ItemStackHelper.getItemPrototype(itemPrototype);
            if (itemPrototype.isEmpty()) {
                reset(notify);
                return this;
            }

            protoStack = itemPrototype;
            protoStack.setCount(1);
            count = 0;

            IDrawerAttributes attrs = getAttributes();
            if (attrs.isDictConvertible())
                matcher = new ItemStackTagMatcher(protoStack);
            else
                matcher = new ItemStackMatcher(protoStack);

            group.syncSlots();
            if (notify)
                onItemChanged();

            return this;
        }

        protected IDrawer setStoredItemRaw (@NotNull ItemStack itemPrototype) {
            itemPrototype = ItemStackHelper.getItemPrototype(itemPrototype);
            protoStack = itemPrototype;
            protoStack.setCount(1);
            count = 0;

            IDrawerAttributes attrs = getAttributes();
            if (attrs.isDictConvertible())
                matcher = new ItemStackTagMatcher(protoStack);
            else
                matcher = new ItemStackMatcher(protoStack);

            return this;
        }

        @Override
        public int getStoredItemCount () {
            if (isMissing() || protoStack.isEmpty())
                return 0;

            IDrawerAttributes attrs = getAttributes();
            if (attrs.isUnlimitedVending())
                return Integer.MAX_VALUE;

            return count;
        }

        @Override
        public void setStoredItemCount (int amount) {
            setStoredItemCount(amount, true);
        }

        protected void setStoredItemCount (int amount, boolean notify) {
            if (isMissing() || protoStack.isEmpty() || count == amount)
                return;

            IDrawerAttributes attrs = getAttributes();
            if (attrs.isUnlimitedVending())
                return;

            count = Math.min(amount, getMaxCapacity());
            count = Math.max(count, 0);

            if (count == 0 && !attrs.isItemLocked(LockAttribute.LOCK_POPULATED))
                reset(notify);
            else {
                if (notify)
                    onAmountChanged();
            }
        }

        protected void setStoredItemCountRaw (int amount) {
            count = amount;
        }

        @Override
        public int adjustStoredItemCount (int amount) {
            return adjustStoredItemCount(amount, true);
        }

        protected int adjustStoredItemCount (int amount, boolean notify) {
            if (isMissing() || protoStack.isEmpty() || amount == 0)
                return Math.abs(amount);

            IDrawerAttributes attrs = getAttributes();
            if (amount > 0) {
                if (attrs.isUnlimitedVending())
                    return 0;

                int originalCount = count;
                count = Math.min(count + amount, getMaxCapacity());

                if (count != originalCount && notify)
                    onAmountChanged();

                if (attrs.isVoid())
                    return 0;

                return amount - (count - originalCount);
            }
            else {
                if (attrs.isUnlimitedVending())
                    return 0;

                int originalCount = count;
                setStoredItemCount(originalCount + amount, notify);

                return -amount - (originalCount - count);
            }
        }

        @Override
        public int getMaxCapacity (@NotNull ItemStack itemPrototype) {
            IDrawerAttributes attrs = getAttributes();
            if (attrs.isUnlimitedStorage() || attrs.isUnlimitedVending())
                return Integer.MAX_VALUE;

            if (itemPrototype.isEmpty())
                return 64 * getStackCapacity();

            try {
                return Math.multiplyExact(getStackSize(itemPrototype), getStackCapacity());
            } catch (ArithmeticException e) {
                return Integer.MAX_VALUE;
            }
        }

        public static int getStackSize (@NotNull ItemStack itemPrototype) {
            if (itemPrototype.isEmpty())
                return 64;

            return itemPrototype.getItem().getDefaultMaxStackSize();
        }

        @Override
        public int getAcceptingMaxCapacity (@NotNull ItemStack itemPrototype) {
            if (isMissing())
                return 0;

            IDrawerAttributes attrs = getAttributes();
            if (attrs.isVoid())
                return Integer.MAX_VALUE;

            return getMaxCapacity(itemPrototype);
        }

        @Override
        public int getRemainingCapacity () {
            if (isMissing() || protoStack.isEmpty())
                return 0;

            IDrawerAttributes attrs = getAttributes();
            if (attrs.isUnlimitedVending())
                return Integer.MAX_VALUE;

            return getMaxCapacity() - getStoredItemCount();
        }

        @Override
        public int getAcceptingRemainingCapacity () {
            if (isMissing() || protoStack.isEmpty())
                return 0;

            IDrawerAttributes attrs = getAttributes();
            if (attrs.isUnlimitedVending() || attrs.isVoid())
                return Integer.MAX_VALUE;

            return getMaxCapacity() - getStoredItemCount();
        }

        @Override
        public boolean canItemBeStored (@NotNull ItemStack itemPrototype, Predicate<ItemStack> matchPredicate) {
            if (isMissing())
                return false;

            IDrawerAttributes attrs = getAttributes();
            if (protoStack.isEmpty() && !attrs.isItemLocked(LockAttribute.LOCK_EMPTY))
                return true;

            if (matchPredicate == null)
                return matcher.matches(itemPrototype);
            return matchPredicate.test(protoStack);
        }

        @Override
        public boolean canItemBeExtracted (@NotNull ItemStack itemPrototype, Predicate<ItemStack> matchPredicate) {
            if (isMissing() || protoStack.isEmpty())
                return false;

            if (matchPredicate == null)
                return matcher.matches(itemPrototype);
            return matchPredicate.test(protoStack);
        }

        @Override
        public boolean isEmpty () {
            return isMissing() || protoStack.isEmpty();
        }

        protected void reset (boolean notify) {
            protoStack = ItemStack.EMPTY;
            count = 0;
            matcher = ItemStackMatcher.EMPTY;

            group.syncSlots();
            if (notify)
                onItemChanged();
        }

        public CompoundTag serializeNBT (HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Missing", missing);

            if (protoStack.isEmpty())
                return tag;

            CompoundTag item = new CompoundTag();
            item = (CompoundTag)protoStack.save(provider, item);

            tag.put("Item", item);
            tag.putInt("Count", count);

            return tag;
        }

        public void deserializeNBT (HolderLookup.Provider provider, CompoundTag nbt) {
            ItemStack tagItem = ItemStack.EMPTY;
            int tagCount = 0;
            boolean tagMissing = false;

            if (nbt.contains("Item"))
                tagItem = ItemStack.parseOptional(provider, nbt.getCompound("Item"));
            if (nbt.contains("Count"))
                tagCount = nbt.getInt("Count");
            if (nbt.contains("Missing"))
                tagMissing = nbt.getBoolean("Missing");

            setStoredItemRaw(tagItem);
            setStoredItemCountRaw(tagCount);
            this.missing = tagMissing;
        }

        public void syncAttributes () {
            if (!protoStack.isEmpty()) {
                //if (attrs.isDictConvertible())
                //    matcher = new ItemStackOreMatcher(protoStack);
                //else
                    matcher = new ItemStackMatcher(protoStack);
            }
        }

        protected int getStackCapacity() {
            return 0;
        }

        protected void onItemChanged() { }

        protected void onAmountChanged() { }

        @Override
        public boolean canDetach () {
            return true;
        }

        @Override
        public boolean isMissing () {
            return missing;
        }

        @Override
        public void setDetached (boolean state) {
            if (missing != state) {
                if (state)
                    setStoredItem(ItemStack.EMPTY);

                missing = state;

                onItemChanged();
                onAmountChanged();
            }
        }

        @Override
        public IDrawer copy () {
            return new DrawerData(this);
        }
    }

}
