package com.jaquadro.minecraft.storagedrawers.item;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributesModifiable;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import net.minecraft.world.item.Item;


public class ItemDrawerKey extends ItemKey
{
    public ItemDrawerKey (Item.Properties properties) {
        super(properties);
    }

    @Override
    protected void handleDrawerAttributes (IDrawerAttributesModifiable attrs) {
        boolean locked = attrs.isItemLocked(LockAttribute.LOCK_POPULATED);
        attrs.setItemLocked(LockAttribute.LOCK_EMPTY, !locked);
        attrs.setItemLocked(LockAttribute.LOCK_POPULATED, !locked);
    }
}
