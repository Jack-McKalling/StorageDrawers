package com.jaquadro.minecraft.storagedrawers.integration;

import com.jaquadro.minecraft.storagedrawers.ModConstants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

@EventBusSubscriber(modid = ModConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class IntegrationEvents
{
    @SubscribeEvent
    public static void enqueueIMC(final InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("theoneprobe"))
            TheOneProbeLoader.sendIMC();
    }
}
