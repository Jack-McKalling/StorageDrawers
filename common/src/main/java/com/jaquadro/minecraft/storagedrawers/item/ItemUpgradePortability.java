package com.jaquadro.minecraft.storagedrawers.item;

import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;

public class ItemUpgradePortability extends ItemUpgrade
{
    public ItemUpgradePortability (Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnabled () {
        return ModCommonConfig.INSTANCE.GENERAL.heavyDrawers.get()
            && ModCommonConfig.INSTANCE.UPGRADES.enablePortabilityUpgrade.get();
    }
}
