package com.jaquadro.minecraft.storagedrawers.block.tile.modelprops;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.IProtectable;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.MaterialData;
import com.jaquadro.minecraft.storagedrawers.client.model.context.DrawerModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.ModelContextSupplier;
import com.jaquadro.minecraft.storagedrawers.components.item.FrameData;
import com.jaquadro.minecraft.storagedrawers.core.ModDataComponents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public class DrawerModelProperties implements ModelContextSupplier<DrawerModelContext>
{
    public static final DrawerModelProperties INSTANCE = new DrawerModelProperties();

    public static final ModelProperty<IDrawerAttributes> ATTRIBUTES = new ModelProperty<>();
    public static final ModelProperty<IDrawerGroup> DRAWER_GROUP = new ModelProperty<>();
    public static final ModelProperty<IProtectable> PROTECTABLE = new ModelProperty<>();
    public static final ModelProperty<MaterialData> MATERIAL = new ModelProperty<>();

    public static ModelData getModelData (BlockEntityDrawers blockEntity) {
        return ModelData.builder()
            .with(ATTRIBUTES, blockEntity.getDrawerAttributes())
            .with(DRAWER_GROUP, blockEntity.getGroup())
            .with(PROTECTABLE, blockEntity)
            .with(MATERIAL, blockEntity.material()).build();
    }

    @Override
    public DrawerModelContext makeContext (@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType type) {
        return new DrawerModelContext(state, side, rand, type)
            .attr(extraData.get(ATTRIBUTES))
            .group(extraData.get(DRAWER_GROUP))
            .protectable(extraData.get(PROTECTABLE))
            .materialData(extraData.get(MATERIAL));
    }

    @Override
    public DrawerModelContext makeContext (ItemStack stack) {
        MaterialData data = stack.getOrDefault(ModDataComponents.FRAME_DATA.get(), FrameData.EMPTY).asMaterialData();

        Block block = Blocks.AIR;
        if (stack.getItem() instanceof BlockItem blockItem)
            block = blockItem.getBlock();

        return new DrawerModelContext(block.defaultBlockState())
            .materialData(data);
    }
}