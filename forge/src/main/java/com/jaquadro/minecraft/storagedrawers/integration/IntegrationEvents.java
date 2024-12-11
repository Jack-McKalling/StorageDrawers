package com.jaquadro.minecraft.storagedrawers.integration;

import com.jaquadro.minecraft.storagedrawers.ModConstants;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class IntegrationEvents
{
    @SubscribeEvent
    public static void enqueueIMC(final InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("theoneprobe"))
            TheOneProbeLoader.sendIMC();
    }
}
