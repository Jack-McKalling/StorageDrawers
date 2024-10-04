package com.jaquadro.minecraft.storagedrawers.block;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlock;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedSourceBlock;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawersStandard;
import com.jaquadro.minecraft.storagedrawers.block.tile.util.FrameHelper;
import com.jaquadro.minecraft.storagedrawers.core.ModBlocks;
import com.jaquadro.minecraft.storagedrawers.util.ItemStackMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BlockStandardDrawers extends BlockDrawers implements IFramedSourceBlock
{
    private String matKey = null;
    private String matNamespace = StorageDrawers.MOD_ID;

    public BlockStandardDrawers (int drawerCount, boolean halfDepth, int storageUnits, BlockBehaviour.Properties properties) {
       super(drawerCount, halfDepth, storageUnits, properties);
    }

    public BlockStandardDrawers (int drawerCount, boolean halfDepth, BlockBehaviour.Properties properties) {
        super(drawerCount, halfDepth, calcUnits(drawerCount, halfDepth), properties);
    }

    private static int calcUnits (int drawerCount, boolean halfDepth) {
        return halfDepth ? 4 / drawerCount : 8 / drawerCount;
    }

    public BlockStandardDrawers setMatKey (ResourceLocation material) {
        this.matNamespace = material.getNamespace();
        this.matKey = material.getPath();
        return this;
    }

    public BlockStandardDrawers setMatKey (@Nullable String matKey) {
        this.matKey = matKey;
        return this;
    }

    public String getMatKey () {
        return matKey;
    }

    public String getNameMatKey () {
        return "block." + matNamespace + ".mat." + matKey;
    }

    @Override
    protected int getFaceSlot (Direction correctSide, @NotNull Vec3 normalizedHit) {
        if (!hitWithinArea(correctSide, normalizedHit, .0625f, .9375f))
            return super.getFaceSlot(correctSide, normalizedHit);

        if (getDrawerCount() == 1)
            return 0;

        boolean hitTop = hitWithinY(normalizedHit, .5f, 1f);
        if (getDrawerCount() == 2)
            return hitTop ? 0 : 1;

        if (getDrawerCount() == 4) {
            if (hitWithinX(correctSide, normalizedHit, 0, .5f))
                return hitTop ? 0 : 2;
            else
                return hitTop ? 1 : 3;
        }

        return super.getFaceSlot(correctSide, normalizedHit);
    }

    @Override
    @Nullable
    public BlockEntityDrawers newBlockEntity (@NotNull BlockPos pos, @NotNull BlockState state) {
        return BlockEntityDrawersStandard.createEntity(getDrawerCount(), pos, state);
    }

    @Override
    public boolean retrimBlock (Level world, BlockPos pos, ItemStack prototype) {
        if (retrimType() == null)
            return false;

        Block protoBlock = Block.byItem(prototype.getItem());
        if (!(protoBlock instanceof BlockTrim))
            return false;

        BlockTrim trim = (BlockTrim) protoBlock;
        if (trim.getMatKey() == null || Objects.equals(trim.getMatKey(), ""))
            return false;

        var blockList = ModBlocks.getDrawersOfTypeAndSizeAndDepth(BlockStandardDrawers.class, getDrawerCount(), isHalfDepth())
            .filter(b -> b.getMatKey() == trim.getMatKey()).toList();

        if (blockList.size() != 1)
            return false;

        BlockStandardDrawers targetBlock = blockList.get(0);
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof BlockEntityDrawersStandard))
            return false;

        BlockState curState = world.getBlockState(pos);
        BlockEntityDrawersStandard curEntity = (BlockEntityDrawersStandard) entity;
        CompoundTag entityData = curEntity.saveWithoutMetadata();

        BlockState newState = targetBlock.defaultBlockState().setValue(FACING, curState.getValue(FACING));
        world.setBlockAndUpdate(pos, newState);

        BlockEntity newEnt = world.getBlockEntity(pos);
        newEnt.load(entityData);

        return true;
    }

    @Override
    public boolean repartitionBlock (Level world, BlockPos pos, ItemStack prototype) {
        if (retrimType() == null)
            return false;

        Block protoBlock = Block.byItem(prototype.getItem());
        if (!(protoBlock instanceof BlockStandardDrawers))
            return false;

        BlockStandardDrawers targetBlock = (BlockStandardDrawers) protoBlock;
        if (targetBlock.isHalfDepth() != isHalfDepth())
            return false;
        if (targetBlock.getDrawerCount() == getDrawerCount())
            return false;

        BlockEntity sourceEntity = world.getBlockEntity(pos);
        if (!(sourceEntity instanceof BlockEntityDrawersStandard))
            return false;

        BlockEntityDrawersStandard sourceBE = (BlockEntityDrawersStandard) sourceEntity;
        ItemStack firstStack = sourceBE.getGroup().getDrawer(0).getStoredItemPrototype();
        int aggCount = 0;

        for (int i = 0; i < sourceBE.getGroup().getDrawerCount(); i++) {
            IDrawer drawer = sourceBE.getGroup().getDrawer(i);
            ItemStack stack = drawer.getStoredItemPrototype();

            if (firstStack.isEmpty() && !stack.isEmpty())
                firstStack = stack;

            if (!ItemStackMatcher.areItemsEqual(firstStack, stack) && !stack.isEmpty())
                return false;

            aggCount += drawer.getStoredItemCount();
        }

        // Set new block

        BlockState curState = world.getBlockState(pos);
        CompoundTag entityData = sourceEntity.saveWithoutMetadata();

        BlockState newState = targetBlock.defaultBlockState().setValue(FACING, curState.getValue(FACING));
        world.setBlockAndUpdate(pos, newState);

        BlockEntity newEnt = world.getBlockEntity(pos);
        newEnt.load(entityData);

        BlockEntityDrawersStandard targetBE = (BlockEntityDrawersStandard) newEnt;
        int drawerCount = targetBE.getGroup().getDrawerCount();
        int divCount = aggCount / drawerCount;
        int remCount = aggCount - (divCount * drawerCount);
        for (int i = 0; i < drawerCount; i++) {
            int slotCount = divCount;
            if (i < remCount)
                slotCount += 1;
            targetBE.getGroup().getDrawer(i).setStoredItem(firstStack, slotCount);
        }

        return true;
    }

    @Override
    public ItemStack makeFramedItem (ItemStack source, ItemStack matSide, ItemStack matTrim, ItemStack matFront) {
        IFramedBlock frameBlock = switch(getDrawerCount()) {
            case 1 -> isHalfDepth() ? ModBlocks.FRAMED_HALF_DRAWERS_1.get() : ModBlocks.FRAMED_FULL_DRAWERS_1.get();
            case 2 -> isHalfDepth() ? ModBlocks.FRAMED_HALF_DRAWERS_2.get() : ModBlocks.FRAMED_FULL_DRAWERS_2.get();
            case 4 -> isHalfDepth() ? ModBlocks.FRAMED_HALF_DRAWERS_4.get() : ModBlocks.FRAMED_FULL_DRAWERS_4.get();
            default -> null;
        };
        if (frameBlock == null)
            return ItemStack.EMPTY;

        return FrameHelper.makeFramedItem(frameBlock, source, matSide, matTrim, matFront);
    }
}
