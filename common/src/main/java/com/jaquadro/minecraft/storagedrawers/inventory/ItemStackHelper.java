package com.jaquadro.minecraft.storagedrawers.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStackHelper
{
    //private static boolean initialized;
    //private static Field itemField;
    //private static Field itemDamageField;
    //private static Field stackTagCompoundField;
    //private static Field capabilitiesField;

    public static Item getTrueItem (@NotNull ItemStack stack) {
        //if (!initialized)
            return stack.getItem();

        /*try {
            return (Item)itemField.get(stack);
        } catch (IllegalAccessException e) {
            return stack.getItem();
        }*/
    }

    @NotNull
    public static ItemStack getItemPrototype (@NotNull ItemStack stack) {
        //if (!initialized)
            return stack.copy();

        /*try {
            CapabilityDispatcher capabilities = (CapabilityDispatcher) capabilitiesField.get(stack);
            Item item = (Item) itemField.get(stack);
            CompoundNBT stackTagCompound = (CompoundNBT) stackTagCompoundField.get(stack);

            ItemStack proto = new ItemStack(item, 1, capabilities != null ? capabilities.serializeNBT() : null);
            if (stackTagCompound != null)
                proto.setTag(stackTagCompound);

            return proto;
        } catch (IllegalAccessException e) {
            return stack.copy();
        }*/
    }

    public static int getMaxStackSize(ItemStack stack) {
        return stack.getItem().getMaxStackSize();
    }

    @NotNull
    public static ItemStack encodeItemStack (@NotNull ItemStack stack) {
        if (!stack.isEmpty())
            return stack;

        ItemStack proto = getItemPrototype(stack);
        if (proto.isEmpty())
            return stack;

        CompoundTag tag = proto.getTag();
        if (tag == null) {
            tag = new CompoundTag();
            proto.setTag(tag);
        }

        tag.putInt("__storagedrawers_count", stack.getCount());

        return proto;
    }

    public static ItemStack encodeItemStack (@NotNull ItemStack proto, int count) {
        if (!proto.isEmpty() && count > 0 && count < 128) {
            ItemStack stack = proto.copy();
            stack.setCount(count);
            return stack;
        }

        if (count == 0 || count >= 128) {
            ItemStack stack = proto.copy();

            CompoundTag tag = stack.getTag();
            if (tag == null) {
                tag = new CompoundTag();
                stack.setTag(tag);
            }

            tag.putInt("__storagedrawers_count", count);
            return stack;
        }

        return proto.copy();
    }

    public static ItemStack decodeItemStack (@NotNull ItemStack stack) {
        int count = ItemStackHelper.decodedCount(stack);
        ItemStack decode = ItemStackHelper.stripDecoding(stack);
        decode.setCount(count);
        return decode;
    }

    public static ItemStack decodeItemStackPrototype (@NotNull ItemStack stack) {
        ItemStack decode = ItemStackHelper.stripDecoding(stack);
        decode.setCount(1);
        return decode;
    }

    public static int decodedCount (@NotNull ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("__storagedrawers_count"))
            return tag.getInt("__storagedrawers_count");

        return stack.getCount();
    }

    public static ItemStack stripDecoding (@NotNull ItemStack stack) {
        ItemStack decode = stack.copy();
        CompoundTag tag = decode.getTag();

        if (tag != null && tag.contains("__storagedrawers_count")) {
            tag.remove("__storagedrawers_count");
            if (tag.size() == 0)
                decode.setTag(null);
            else
                decode.setTag(tag);
        }

        return decode;
    }

    public static boolean isStackEncoded (@NotNull ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null)
            return false;

        return tag.contains("__storagedrawers_count");
    }

    /*static {
        try {
            itemField = ReflectionHelper.findField(ItemStack.class,  "item", "item");
            itemDamageField = ReflectionHelper.findField(ItemStack.class,"itemDamage", "field_77991_e");
            stackTagCompoundField = ReflectionHelper.findField(ItemStack.class,"stackTagCompound", "tag");
            capabilitiesField = ReflectionHelper.findField(ItemStack.class,"capabilities");

            itemField.setAccessible(true);
            itemDamageField.setAccessible(true);
            stackTagCompoundField.setAccessible(true);
            capabilitiesField.setAccessible(true);

            initialized = true;
        } catch (ReflectionHelper.UnableToFindFieldException e) {
            initialized = false;
        }
    }*/
}
