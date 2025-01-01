package com.jaquadro.minecraft.storagedrawers.client.model;

import com.jaquadro.minecraft.storagedrawers.ModConstants;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.DrawerModelProperties;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.FramedModelProperties;
import com.jaquadro.minecraft.storagedrawers.client.model.context.DrawerModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.context.FramedModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.decorator.CombinedModelDecorator;
import com.jaquadro.minecraft.storagedrawers.client.model.decorator.DrawerModelDecorator;
import com.jaquadro.minecraft.storagedrawers.client.model.decorator.MaterialModelDecorator;
import com.jaquadro.minecraft.storagedrawers.core.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class ModelLoadPlugin implements ModelLoadingPlugin
{
    public class UnbakedProxyModel implements UnbakedBlockStateModel {

        UnbakedBlockStateModel parent;
        ModelResourceLocation modelLoc;

        public UnbakedProxyModel(UnbakedBlockStateModel parent, ModelResourceLocation modelLoc) {
            this.parent = parent;
            this.modelLoc = modelLoc;
        }

        @Override
        public BakedModel bake (ModelBaker modelBaker) {
            BakedModel original = parent.bake(modelBaker);

            ResourceLocation blockId = modelLoc.id();
            DrawerModelStore.tryAddModel(modelLoc, original);
            if (!DrawerModelStore.INSTANCE.isTargetedModel(modelLoc))
                return original;

            BakedModel proxyModel;

            if (blockId.equals(ModBlocks.FRAMED_FULL_DRAWERS_1.getId()))
                proxyModel = BakedModelProvider.makeFramedStandardDrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_FULL_DRAWERS_2.getId()))
                proxyModel = BakedModelProvider.makeFramedStandardDrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_FULL_DRAWERS_4.getId()))
                proxyModel = BakedModelProvider.makeFramedStandardDrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_HALF_DRAWERS_1.getId()))
                proxyModel = BakedModelProvider.makeFramedStandardDrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_HALF_DRAWERS_2.getId()))
                proxyModel = BakedModelProvider.makeFramedStandardDrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_HALF_DRAWERS_4.getId()))
                proxyModel = BakedModelProvider.makeFramedStandardDrawerModel(original);

            else if (blockId.equals(ModBlocks.FRAMED_COMPACTING_DRAWERS_2.getId()))
                proxyModel = BakedModelProvider.makeFramedComp2DrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_COMPACTING_HALF_DRAWERS_2.getId()))
                proxyModel = BakedModelProvider.makeFramedComp2DrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_COMPACTING_DRAWERS_3.getId()))
                proxyModel = BakedModelProvider.makeFramedComp3DrawerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_COMPACTING_HALF_DRAWERS_3.getId()))
                proxyModel = BakedModelProvider.makeFramedComp3DrawerModel(original);

            else if (blockId.equals(ModBlocks.FRAMED_TRIM.getId()))
                proxyModel = BakedModelProvider.makeFramedTrimModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_CONTROLLER.getId()))
                proxyModel = BakedModelProvider.makeFramedControllerModel(original);
            else if (blockId.equals(ModBlocks.FRAMED_CONTROLLER_IO.getId()))
                proxyModel = BakedModelProvider.makeFramedControllerIOModel(original);

            else
                proxyModel = BakedModelProvider.makeStandardDrawerModel(original);

            ItemModelStore.models.put(modelLoc, proxyModel);

            return proxyModel;
        }

        @Override
        public Object visualEqualityGroup (BlockState blockState) {
            return parent.visualEqualityGroup(blockState);
        }

        @Override
        public void resolveDependencies (Resolver resolver) {
            parent.resolveDependencies(resolver);
        }
    }

    @Override
    public void initialize (Context pluginContext) {
        DrawerModelGeometry.loadGeometryData();
        pluginContext.modifyBlockModelOnLoad().register((original, context) -> {
            if (context.id() == null)
                return original;

            ResourceLocation blockId = context.id().id();
            if (!blockId.getNamespace().equals(ModConstants.MOD_ID))
                return original;

            if (original instanceof UnbakedProxyModel)
                return original;

            return new UnbakedProxyModel(original, context.id());
        });

        ItemModels.ID_MAPPER.put(
            ModConstants.loc("framed_block"), PlatformDecoratedModel.PlatformDecoratedItemModel.Unbaked.MAP_CODEC
        );
    }
}
