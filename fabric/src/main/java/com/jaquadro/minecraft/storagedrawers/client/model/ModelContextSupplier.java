package com.jaquadro.minecraft.storagedrawers.client.model;

import com.jaquadro.minecraft.storagedrawers.client.model.context.ModelContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface ModelContextSupplier<C extends ModelContext>
{
    C makeContext(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, Object renderData, @Nullable RenderType type);

    C makeContext(ItemStack stack);
}