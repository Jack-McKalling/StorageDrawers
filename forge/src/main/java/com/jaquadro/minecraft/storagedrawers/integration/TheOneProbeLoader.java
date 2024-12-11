package com.jaquadro.minecraft.storagedrawers.integration;

import net.minecraftforge.fml.InterModComms;

public class TheOneProbeLoader
{
    public static void sendIMC () {
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", TheOneProbe::new);
    }
}
