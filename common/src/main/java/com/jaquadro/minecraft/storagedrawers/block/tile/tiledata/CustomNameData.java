package com.jaquadro.minecraft.storagedrawers.block.tile.tiledata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

public class CustomNameData extends BlockEntityDataShim
{
    private Component customName;

    public CustomNameData () { }

    @Override
    public void read (CompoundTag tag) {
        customName = null;
        if (tag.contains("CustomName", Tag.TAG_STRING))
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
    }

    @Override
    public CompoundTag write (CompoundTag tag) {
        if (hasCustomName())
            tag.putString("CustomName", Component.Serializer.toJson(customName));

        return tag;
    }

    public String getName () {
        return hasCustomName() ? customName.getString() : "";
    }

    public boolean hasCustomName () {
        return customName != null;
    }

    public Component getDisplayName () {
        return hasCustomName() ? customName : null;
    }

    public void setName (Component name) {
        customName = name;
    }
}