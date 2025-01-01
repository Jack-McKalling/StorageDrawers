package com.jaquadro.minecraft.storagedrawers.client.model;

import com.jaquadro.minecraft.storagedrawers.client.model.context.ModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.decorator.ModelDecorator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class PlatformDecoratedModel<C extends ModelContext> extends ParentModel implements FabricBakedModel
{
    private final ModelDecorator<C> decorator;
    private final ModelContextSupplier<C> contextSupplier;
    private final ItemStack stack;

    private static Map<BakedModel, Mesh> meshCache = new HashMap<>();
    private static RenderMaterial cutoutMat;
    private static RenderMaterial transMat;

    public PlatformDecoratedModel (BakedModel parent, ModelDecorator<C> decorator, ModelContextSupplier<C> contextSupplier) {
        super(parent);
        this.decorator = decorator;
        this.contextSupplier = contextSupplier;
        this.stack = null;
    }

    public PlatformDecoratedModel (PlatformDecoratedModel<C> p, ItemStack stack) {
        super(p.parent);
        this.decorator = p.decorator;
        this.contextSupplier = p.contextSupplier;
        this.stack = stack;
    }

    @Override
    public boolean isVanillaAdapter () {
        return false;
    }

    @Override
    public void emitItemQuads (QuadEmitter emitter, Supplier<RandomSource> randomSupplier) {
        Supplier<C> supplier = () -> contextSupplier.makeContext(stack);

        if (decorator.shouldRenderBase(supplier, stack))
            parent.emitItemQuads(emitter, randomSupplier);

        RandomSource randomSource = randomSupplier.get();

        BiConsumer<BakedModel, RenderType> emitModel = (model, renderType) -> {
            if (model != null) {
                if (renderType == RenderType.translucent()) {
                    if (stack.getItem() instanceof BlockItem bi) {
                        Mesh mesh = getMesh(model, bi.getBlock().defaultBlockState(), randomSource, renderType);
                        mesh.outputTo(emitter);
                    }
                } else
                    model.emitItemQuads(emitter, randomSupplier);
            }
        };

        try {
            decorator.emitItemQuads(supplier, emitModel, stack);
        } catch (Exception e) { }
    }

    @Override
    public void emitBlockQuads (QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest) {
        if (state == null) {
            parent.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
            return;
        }

        FabricBlockView fabricView = blockView;
        if (fabricView == null)
            return;

        RandomSource randomSource = randomSupplier.get();

        Object renderData = fabricView.getBlockEntityRenderData(pos);
        Supplier<C> supplier = () -> contextSupplier.makeContext(state, null, randomSource, renderData, null);

        if (decorator.shouldRenderBase(supplier))
            parent.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);

        BiConsumer<BakedModel, RenderType> emitModel = (model, renderType) -> {
            if (model != null) {
                if (renderType == RenderType.translucent()) {
                    Mesh mesh = getMesh(model, state, randomSource, renderType);
                    mesh.outputTo(emitter);
                } else
                    model.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
            }
        };

        try {
            decorator.emitQuads(supplier, emitModel);
        } catch (Exception e) { }
    }

    private Mesh getMesh (BakedModel model, BlockState state, RandomSource randomSource, RenderType renderType) {
        if (meshCache.containsKey(model))
            return meshCache.get(model);

        Mesh mesh = buildMesh(model, state, randomSource, renderType);
        meshCache.put(model, mesh);
        return mesh;
    }

    private Mesh buildMesh (BakedModel model, BlockState state, RandomSource randomSource, RenderType renderType) {
        Renderer render = Renderer.get();
        RenderMaterial mat = null;

        if (renderType == RenderType.cutoutMipped()) {
            if (cutoutMat == null)
                cutoutMat = render.materialFinder().blendMode(BlendMode.CUTOUT_MIPPED).find();
            mat = cutoutMat;
        }
        else if (renderType == RenderType.translucent()) {
            if (transMat == null)
                transMat = render.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
            mat = transMat;
        }

        if (mat == null)
            return null;

        MutableMesh builder = render.mutableMesh();
        QuadEmitter quadEmit = builder.emitter();

        for (var d : Direction.values()) {
            for (var quad : model.getQuads(state, d, randomSource)) {
                quadEmit.fromVanilla(quad, mat, d).emit();
            }
        }
        for (var quad : model.getQuads(state, null, randomSource)) {
            quadEmit.fromVanilla(quad, mat, null).emit();
        }

        return builder.immutableCopy();
    }

    public static class PlatformDecoratedItemModel implements ItemModel
    {
        ModelResourceLocation location;
        PlatformDecoratedModel<? extends ModelContext> parent;
        BakedModel model;
        ItemStack stack;

        public PlatformDecoratedItemModel (ModelResourceLocation location) {
            this.location = location;
        }

        @Override
        public void update (ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i) {
            if (parent == null) {
                BakedModel stored = ItemModelStore.models.get(location);
                if (stored instanceof PlatformDecoratedModel<?> p)
                    parent = p;
            }

            if ((stack == null || !ItemStack.isSameItemSameComponents(stack, itemStack)) && parent != null) {
                stack = itemStack;
                model = new PlatformDecoratedModel<>(parent, itemStack);
            }

            if (model != null) {
                ItemStackRenderState.LayerRenderState renderState = itemStackRenderState.newLayer();
                renderState.setupBlockModel(model, RenderType.cutoutMipped());
            }
        }

        public record Unbaked (ResourceLocation model, String variant) implements ItemModel.Unbaked {
            public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec((builder) ->
                builder.group(
                    ResourceLocation.CODEC.fieldOf("model").forGetter(PlatformDecoratedItemModel.Unbaked::model),
                    Codec.STRING.fieldOf("variant").forGetter(PlatformDecoratedItemModel.Unbaked::variant)
                ).apply(builder, PlatformDecoratedItemModel.Unbaked::new)
            );

            @Override
            public MapCodec<? extends ItemModel.Unbaked> type () {
                return MAP_CODEC;
            }

            @Override
            public ItemModel bake (BakingContext bakingContext) {
                ModelResourceLocation loc = new ModelResourceLocation(model, variant);
                return new PlatformDecoratedItemModel(loc);
            }

            @Override
            public void resolveDependencies (Resolver resolver) {
                resolver.resolve(model);
            }
        }
    }
}
