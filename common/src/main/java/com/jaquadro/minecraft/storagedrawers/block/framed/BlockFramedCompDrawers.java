package com.jaquadro.minecraft.storagedrawers.block.framed;

import com.jaquadro.minecraft.storagedrawers.ModServices;
import com.jaquadro.minecraft.storagedrawers.api.framing.FrameMaterial;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlock;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlockEntity;
import com.jaquadro.minecraft.storagedrawers.block.BlockCompDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityControllerIO;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawersComp;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawersStandard;
import com.jaquadro.minecraft.storagedrawers.components.item.FrameData;
import com.jaquadro.minecraft.storagedrawers.core.ModDataComponents;
import com.jaquadro.minecraft.storagedrawers.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockFramedCompDrawers extends BlockCompDrawers implements IFramedBlock
{
    public BlockFramedCompDrawers (int drawerCount, boolean halfDepth, int storageUnits, Properties properties) {
        super(drawerCount, halfDepth, storageUnits, properties);
    }

    public BlockFramedCompDrawers (int drawerCount, boolean halfDepth, Properties properties) {
        super(drawerCount, halfDepth, properties);
    }

    public void setPlacedBy (@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);

        BlockEntityDrawersComp blockEntity = WorldUtils.getBlockEntity(world, pos, BlockEntityDrawersComp.class);
        if (blockEntity == null)
            return;

        blockEntity.material().read(stack);
        blockEntity.setChanged();
    }

    @Override
    protected ItemStack getMainDrop (BlockState state, BlockEntityDrawers tile) {
        ItemStack drop = super.getMainDrop(state, tile);

        if (!tile.material().isEmpty())
            drop.set(ModDataComponents.FRAME_DATA.get(), new FrameData(tile.material()));

        return drop;
    }

    @Override
    public IFramedBlockEntity getFramedBlockEntity (@NotNull Level world, @NotNull BlockPos pos) {
        return WorldUtils.getBlockEntity(world, pos, BlockEntityDrawersComp.class);
    }

    @Override
    public boolean supportsFrameMaterial (FrameMaterial material) {
        return true;
    }
}