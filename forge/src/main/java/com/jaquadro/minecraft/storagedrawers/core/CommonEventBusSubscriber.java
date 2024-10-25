package com.jaquadro.minecraft.storagedrawers.core;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.block.BlockDrawers;
import com.jaquadro.minecraft.storagedrawers.config.ModClientConfig;
import com.jaquadro.minecraft.storagedrawers.inventory.SlotUpgrade;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeStorage;
import com.texelsaurus.minecraft.chameleon.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = StorageDrawers.MOD_ID)
public class CommonEventBusSubscriber {
    @SubscribeEvent
    public static void playerLeftClick (@NotNull PlayerInteractEvent.LeftClickBlock event) {
        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BlockDrawers blockDrawers) {
            Player player = event.getEntity();
            if (player.isCreative()) {
                BlockHitResult hit = WorldUtils.rayTraceEyes(level, player, pos);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    blockDrawers.leftAction(state, level, pos, player, hit);
                    event.setCanceled(blockDrawers.getFaceSlot(state, hit) >= 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void itemStackedOn (ItemStackedOnOtherEvent event) {
        if (!(event.getStackedOnItem().getItem() instanceof ItemUpgradeStorage))
            return;
        if (!(event.getCarriedItem().getItem() instanceof ItemUpgradeStorage))
            return;

        if (event.getSlot() instanceof SlotUpgrade slot) {
            ItemStack stacked = event.getStackedOnItem();
            ItemStack carried = event.getCarriedItem();
            ItemStack slotStack = event.getSlot().getItem();

            // NB: Forge 1.20.1 arguments for stacked and carried items are swapped
            if (ItemStack.matches(slotStack, carried) && !ItemStack.matches(slotStack, stacked)) {
                carried = event.getStackedOnItem();
                stacked = event.getCarriedItem();
            }

            if (slot.canSwapStack(carried)) {
                event.getSlot().set(carried);
                event.getCarriedSlotAccess().set(stacked);
                event.setCanceled(true);
            }
        }
    }
}
