package com.jaquadro.minecraft.storagedrawers.item;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.IPortable;
import com.jaquadro.minecraft.storagedrawers.block.BlockDrawers;
import com.jaquadro.minecraft.storagedrawers.block.BlockStandardDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemDrawers extends BlockItem implements IPortable
{
    public ItemDrawers (Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText (@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        //if (stack.hasTag() && stack.getTag().contains("material")) {
        //    String key = stack.getTag().getString("material");
        //    tooltip.add(new TranslationTextComponent("storagedrawers.material", I18n.format("storagedrawers.material." + key)));
        //}

        Component textCapacity = Component.translatable("tooltip.storagedrawers.drawers.capacity", getCapacityForBlock(stack));
        tooltip.add(Component.literal("").append(textCapacity).withStyle(ChatFormatting.GRAY));

        CompoundTag tag = stack.getTagElement("tile");
        if (tag != null) {
            Component textSealed = Component.translatable("tooltip.storagedrawers.drawers.sealed");
            tooltip.add(Component.literal("").append(textSealed).withStyle(ChatFormatting.YELLOW));
        }

        if (ModCommonConfig.INSTANCE.GENERAL.heavyDrawers.get() && isHeavy(stack)) {
            tooltip.add(Component.translatable("tooltip.storagedrawers.drawers.too_heavy").withStyle(ChatFormatting.RED));
        }

        //tooltip.add(getDescription().applyTextStyle(TextFormatting.GRAY));
    }

    @Override
    public Component getName (ItemStack stack) {
        String fallback = null;
        Block block = Block.byItem(stack.getItem());

        if (block instanceof BlockStandardDrawers drawers) {
            String matKey = drawers.getMatKey();
            if (matKey != null) {
                String mat = Component.translatable(drawers.getNameMatKey()).getString();
                fallback = Component.translatable(drawers.getNameTypeKey(), mat).getString();
            }
        }

        return Component.translatableWithFallback(this.getDescriptionId(stack), fallback);
    }

    @NotNull
    public Component getDescription() {
        return Component.translatable(this.getDescriptionId() + ".desc");
    }

    @Override
    public boolean isHeavy(@NotNull ItemStack stack) {
        if (stack.getItem() != this)
            return false;

        CompoundTag tile = stack.getTagElement("tile");
        if(tile == null || tile.isEmpty())
            return false;

        var x = new UpgradeData(7);
        try {
            x.read(tile);
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

    // TODO: Forge Extension
    // @Override
    public boolean doesSneakBypassUse (ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        return block instanceof BlockDrawers bd && bd.retrimType() != null;
    }
}
