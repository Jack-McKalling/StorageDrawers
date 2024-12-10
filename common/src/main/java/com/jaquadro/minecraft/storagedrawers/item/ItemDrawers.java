package com.jaquadro.minecraft.storagedrawers.item;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.IPortable;
import com.jaquadro.minecraft.storagedrawers.block.BlockDrawers;
import com.jaquadro.minecraft.storagedrawers.block.BlockStandardDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemDrawers extends BlockItem implements IPortable
{
    public ItemDrawers (Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText (ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        //if (stack.hasTag() && stack.getTag().contains("material")) {
        //    String key = stack.getTag().getString("material");
        //    tooltip.add(new TranslationTextComponent("storagedrawers.material", I18n.format("storagedrawers.material." + key)));
        //}

        Component textCapacity = Component.translatable("tooltip.storagedrawers.drawers.capacity", getCapacityForBlock(stack));
        tooltip.add(Component.literal("").append(textCapacity).withStyle(ChatFormatting.GRAY));

        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            Component textSealed = Component.translatable("tooltip.storagedrawers.drawers.sealed");
            tooltip.add(Component.literal("").append(textSealed).withStyle(ChatFormatting.YELLOW));
        }

        if (ModCommonConfig.INSTANCE.GENERAL.heavyDrawers.get() && isHeavy(context.registries(), stack)) {
            tooltip.add(Component.translatable("tooltip.storagedrawers.drawers.too_heavy").withStyle(ChatFormatting.RED));
        }

        //tooltip.add(getDescription().applyTextStyle(TextFormatting.GRAY));
    }

    @Override
    public Component getName (ItemStack stack) {
        Component fallback = Component.empty();
        Block block = Block.byItem(stack.getItem());

        if (block instanceof BlockStandardDrawers drawers) {
            String matKey = drawers.getMatKey();
            if (matKey != null) {
                String mat = Component.translatable(drawers.getNameMatKey()).getString();
                fallback = Component.translatable(drawers.getNameTypeKey(), mat);
            }
        } else
            fallback = super.getName(stack);

        return fallback;
    }

    @NotNull
    public Component getDescription() {
        return Component.translatable(this.getDescriptionId() + ".desc");
    }

    @Override
    public boolean isHeavy(HolderLookup.Provider provider, @NotNull ItemStack stack) {
        if (stack.getItem() != this)
            return false;

        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data.isEmpty())
            return false;

        var x = new UpgradeData(7);
        try {
            x.read(provider, data.copyTag());
        } catch (ClassCastException e) {
            return false;
        }

        return !x.hasPortabilityUpgrade();
    }

    private int getCapacityForBlock (@NotNull ItemStack itemStack) {
        Block block = Block.byItem(itemStack.getItem());
        if (block instanceof BlockDrawers blockDrawers) {
            return blockDrawers.getStorageUnits() * ModCommonConfig.INSTANCE.GENERAL.getBaseStackStorage();
        }

        return 0;
    }

    // TODO: Forge extension
    // @Override
    public boolean doesSneakBypassUse (ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        return block instanceof BlockDrawers bd && bd.retrimType() != null;
    }
}
