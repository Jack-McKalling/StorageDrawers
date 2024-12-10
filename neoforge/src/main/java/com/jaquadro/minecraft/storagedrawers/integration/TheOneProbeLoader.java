package com.jaquadro.minecraft.storagedrawers.integration;

import net.neoforged.fml.InterModComms;

public class TheOneProbeLoader
{
    public static void sendIMC () {
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", TheOneProbe::new);
    }
}
