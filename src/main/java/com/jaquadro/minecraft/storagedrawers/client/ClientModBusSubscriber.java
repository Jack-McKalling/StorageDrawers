package com.jaquadro.minecraft.storagedrawers.client;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.DrawerModelProperties;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.FramedModelProperties;
import com.jaquadro.minecraft.storagedrawers.client.gui.ClientDetachedDrawerTooltip;
import com.jaquadro.minecraft.storagedrawers.client.model.DrawerModelGeometry;
import com.jaquadro.minecraft.storagedrawers.client.model.DrawerModelStore;
import com.jaquadro.minecraft.storagedrawers.client.model.ForgeDecoratedModel;
import com.jaquadro.minecraft.storagedrawers.client.model.context.DrawerModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.context.FramedModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.decorator.*;
import com.jaquadro.minecraft.storagedrawers.client.renderer.BlockEntityDrawersRenderer;
import com.jaquadro.minecraft.storagedrawers.client.renderer.BlockEntityFramingRenderer;
import com.jaquadro.minecraft.storagedrawers.core.ModBlockEntities;
import com.jaquadro.minecraft.storagedrawers.core.ModBlocks;
import com.jaquadro.minecraft.storagedrawers.core.ModContainers;
import com.jaquadro.minecraft.storagedrawers.inventory.DrawerScreen;
import com.jaquadro.minecraft.storagedrawers.inventory.FramingTableScreen;
import com.jaquadro.minecraft.storagedrawers.inventory.tooltip.DetachedDrawerTooltip;
import com.jaquadro.minecraft.storagedrawers.inventory.tooltip.KeyringTooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = StorageDrawers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModBusSubscriber {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            /*
            I'd like to do this registration in bulk as well, but I'm hesitant to put
            client-only code in common classes like ModContainers (memorizing screen types).
            BERs at least all have the same renderer.
            */
            MenuScreens.register(ModContainers.DRAWER_CONTAINER_1.get(), DrawerScreen.Slot1::new);
            MenuScreens.register(ModContainers.DRAWER_CONTAINER_2.get(), DrawerScreen.Slot2::new);
            MenuScreens.register(ModContainers.DRAWER_CONTAINER_4.get(), DrawerScreen.Slot4::new);
            MenuScreens.register(ModContainers.DRAWER_CONTAINER_COMP_2.get(), DrawerScreen.Compacting2::new);
            MenuScreens.register(ModContainers.DRAWER_CONTAINER_COMP_3.get(), DrawerScreen.Compacting3::new);

            MenuScreens.register(ModContainers.FRAMING_TABLE.get(), FramingTableScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerEntityRenderers(RegisterRenderers event) {
        ModBlockEntities.getBlockEntityTypesWithRenderers().forEach(ro -> event.registerBlockEntityRenderer(ro.get(), BlockEntityDrawersRenderer::new));

        event.registerBlockEntityRenderer(ModBlockEntities.FRAMING_TABLE.get(), BlockEntityFramingRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(KeyringTooltip.class, ClientBundleTooltip::new);
        event.register(DetachedDrawerTooltip.class, ClientDetachedDrawerTooltip::new);
    }

    @SubscribeEvent
    public static void registerTextures (TextureStitchEvent event) {
        if (!ModBlocks.OAK_FULL_DRAWERS_1.isPresent()) {
            StorageDrawers.log.warn("Block objects not set in TextureStitchEvent.  Is your mod environment broken?");
            return;
        }

        DrawerModelGeometry.loadGeometryData();
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.ModifyBakingResult event) {
        if (!ModBlocks.OAK_FULL_DRAWERS_1.isPresent()) {
            StorageDrawers.log.warn("Block objects not set in ModelBakeEvent.  Is your mod environment broken?");
            return;
        }

        DrawerModelStore.getModelLocations().forEach(loc -> {
            DrawerModelStore.tryAddModel(loc, event.getModels().get(loc));
        });

        ModBlocks.getDrawers().forEach(blockDrawers -> replaceBlock(event, blockDrawers, ClientModBusSubscriber::makeStandardDrawerModel));
        ModBlocks.getFramedDrawers().forEach(blockDrawers -> replaceBlock(event, blockDrawers, ClientModBusSubscriber::makeFramedStandardDrawerModel));

        replaceBlock(event, ModBlocks.FRAMED_COMPACTING_DRAWERS_2.get(), ClientModBusSubscriber::makeFramedComp2DrawerModel);
        replaceBlock(event, ModBlocks.FRAMED_COMPACTING_HALF_DRAWERS_2.get(), ClientModBusSubscriber::makeFramedComp2DrawerModel);
        replaceBlock(event, ModBlocks.FRAMED_COMPACTING_DRAWERS_3.get(), ClientModBusSubscriber::makeFramedComp3DrawerModel);
        replaceBlock(event, ModBlocks.FRAMED_COMPACTING_HALF_DRAWERS_3.get(), ClientModBusSubscriber::makeFramedComp3DrawerModel);

        replaceBlock(event, ModBlocks.FRAMED_TRIM.get(), ClientModBusSubscriber::makeFramedTrimModel);
        replaceBlock(event, ModBlocks.FRAMED_CONTROLLER.get(), ClientModBusSubscriber::makeFramedControllerModel);
        replaceBlock(event, ModBlocks.FRAMED_CONTROLLER_IO.get(), ClientModBusSubscriber::makeFramedControllerIOModel);

        List.of("framed_full_drawers_4", "framed_full_drawers_2", "framed_full_drawers_1", "framed_half_drawers_4",
            "framed_half_drawers_2", "framed_half_drawers_1").forEach(d -> {
            ModelResourceLocation testResource = new ModelResourceLocation(new ResourceLocation("storagedrawers", d), "inventory");
            BakedModel test = event.getModels().get(testResource);
            if (test != null)
                event.getModels().put(testResource, makeFramedStandardDrawerModel(test));
        });

        List.of("framed_compacting_drawers_2", "framed_compacting_half_drawers_2").forEach(d -> {
            ModelResourceLocation testResource = new ModelResourceLocation(new ResourceLocation("storagedrawers", d), "inventory");
            BakedModel test = event.getModels().get(testResource);
            if (test != null)
                event.getModels().put(testResource, makeFramedComp2DrawerModel(test));
        });

        List.of("framed_compacting_drawers_3", "framed_compacting_half_drawers_3").forEach(d -> {
            ModelResourceLocation testResource = new ModelResourceLocation(new ResourceLocation("storagedrawers", d), "inventory");
            BakedModel test = event.getModels().get(testResource);
            if (test != null)
                event.getModels().put(testResource, makeFramedComp3DrawerModel(test));
        });

        List.of("framed_trim").forEach(d -> {
            ModelResourceLocation testResource = new ModelResourceLocation(new ResourceLocation("storagedrawers", d), "inventory");
            BakedModel test = event.getModels().get(testResource);
            if (test != null)
                event.getModels().put(testResource, makeFramedTrimModel(test));
        });

        List.of("framed_controller").forEach(d -> {
            ModelResourceLocation testResource = new ModelResourceLocation(new ResourceLocation("storagedrawers", d), "inventory");
            BakedModel test = event.getModels().get(testResource);
            if (test != null)
                event.getModels().put(testResource, makeFramedControllerModel(test));
        });

        List.of("framed_controller_io").forEach(d -> {
            ModelResourceLocation testResource = new ModelResourceLocation(new ResourceLocation("storagedrawers", d), "inventory");
            BakedModel test = event.getModels().get(testResource);
            if (test != null)
                event.getModels().put(testResource, makeFramedControllerIOModel(test));
        });
    }

    public static void replaceBlock(ModelEvent.ModifyBakingResult event, Block block, Function<BakedModel, BakedModel> replacer) {
        BakedModel missing = event.getModels().get(ModelBakery.MISSING_MODEL_LOCATION);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation modelResource = BlockModelShaper.stateToModelLocation(state);
            BakedModel parentModel = event.getModels().get(modelResource);
            if (parentModel == null) {
                StorageDrawers.log.warn("Got back null model from ModelBakeEvent.ModelManager for resource " + modelResource.toString());
                continue;
            } else if (parentModel == missing)
                continue;

            if (DrawerModelStore.INSTANCE.isTargetedModel(modelResource)) {
                event.getModels().put(modelResource, replacer.apply(parentModel));
            }
        }
    }

    static BakedModel makeStandardDrawerModel(BakedModel parentModel) {
        DrawerModelDecorator decorator = new DrawerModelDecorator(DrawerModelStore.INSTANCE);
        return new ForgeDecoratedModel<>(parentModel, decorator, DrawerModelProperties.INSTANCE);
    }

    static BakedModel makeFramedDrawerModel (BakedModel parentModel, DrawerModelStore.FrameMatSet matSet) {
        CombinedModelDecorator<DrawerModelContext> decorator = new CombinedModelDecorator<>();
        decorator.add(new DrawerModelDecorator(DrawerModelStore.INSTANCE));
        decorator.add(new MaterialModelDecorator.FacingSizedSlotted<>(matSet, true));

        return new ForgeDecoratedModel<>(parentModel, decorator, DrawerModelProperties.INSTANCE);
    }

    static BakedModel makeFramedStandardDrawerModel(BakedModel parentModel) {
        return makeFramedDrawerModel(parentModel, DrawerModelStore.FramedStandardDrawerMaterials);
    }

    static BakedModel makeFramedCompDrawerModel (BakedModel parentModel, DrawerModelStore.FrameMatSet matSet) {
        CombinedModelDecorator<DrawerModelContext> decorator = new CombinedModelDecorator<>();
        decorator.add(new DrawerModelDecorator(DrawerModelStore.INSTANCE));
        decorator.add(new MaterialModelDecorator.FacingSizedOpen<>(matSet, true));

        return new ForgeDecoratedModel<>(parentModel, decorator, DrawerModelProperties.INSTANCE);
    }

    static BakedModel makeFramedComp2DrawerModel(BakedModel parentModel) {
        return makeFramedCompDrawerModel(parentModel, DrawerModelStore.FramedComp2DrawerMaterials);
    }

    static BakedModel makeFramedComp3DrawerModel(BakedModel parentModel) {
        return makeFramedCompDrawerModel(parentModel, DrawerModelStore.FramedComp3DrawerMaterials);
    }

    static BakedModel makeFramedTrimModel(BakedModel parentModel) {
        MaterialModelDecorator<FramedModelContext> decorator =
            new MaterialModelDecorator.Single<>(DrawerModelStore.FramedTrimMaterials, true);
        return new ForgeDecoratedModel<>(parentModel, decorator, FramedModelProperties.INSTANCE);
    }

    static BakedModel makeFramedControllerModel(BakedModel parentModel) {
        MaterialModelDecorator<FramedModelContext> decorator =
            new MaterialModelDecorator.Facing<>(DrawerModelStore.FramedControllerMaterials, true);
        return new ForgeDecoratedModel<>(parentModel, decorator, FramedModelProperties.INSTANCE);
    }

    static BakedModel makeFramedControllerIOModel(BakedModel parentModel) {
        MaterialModelDecorator<FramedModelContext> decorator =
            new MaterialModelDecorator.Single<>(DrawerModelStore.FramedControllerIOMaterials, true);
        return new ForgeDecoratedModel<>(parentModel, decorator, FramedModelProperties.INSTANCE);
    }
}
