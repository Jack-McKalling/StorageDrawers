package com.jaquadro.minecraft.storagedrawers.core;

import com.jaquadro.minecraft.storagedrawers.ModConstants;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegisterEvent;

public class ModCreativeTabs
{
    private static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(ModConstants.MOD_ID, "storagedrawers"));

    public static void init (RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(MAIN, CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.OAK_FULL_DRAWERS_2.get()))
                .title(Component.translatable("itemGroup.storagedrawers"))
                .displayItems((params, output) -> {
                    ModItems.ITEMS.getEntries().forEach((reg) -> {
                        if (reg == null)
                            return;
                        if (ModItems.EXCLUDE_ITEMS_CREATIVE_TAB.contains(reg))
                            return;
                        if (reg.get() instanceof BlockItem blockItem) {
                            if (blockItem.getBlock() instanceof IFramedBlock)
                                return;
                        }

                        output.accept(reg.get().getDefaultInstance());
                    });
                })
                .build());
        });
    }
}
