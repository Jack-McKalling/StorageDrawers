package com.jaquadro.minecraft.storagedrawers.client.model;

import com.jaquadro.minecraft.storagedrawers.client.model.context.ModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.decorator.ModelDecorator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class PlatformDecoratedModel<C extends ModelContext> extends ParentModel implements IDynamicBakedModel
{
    private final ModelDecorator<C> decorator;
    private final ModelContextSupplier<C> contextSupplier;

    public PlatformDecoratedModel (BakedModel parent, ModelDecorator<C> decorator, ModelContextSupplier<C> contextSupplier) {
        super(parent);
        this.decorator = decorator;
        this.contextSupplier = contextSupplier;
    }

    @Override
    public List<BakedQuad> getQuads (@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType type) {
        if (state == null) {
            // NB: getting here for item renders (state == null) implies that the caller has not
            // respected #getRenderPasses, since if they had this method wouldn't be called.
            // If that's the case, then we might as well return the main quads that they're looking
            // for anyway.
            return parent.getQuads(state, side, rand, extraData, type);
        }

        List<BakedQuad> quads = new ArrayList<>();

        Supplier<C> supplier = () -> contextSupplier.makeContext(state, side, rand, extraData, type);
        if (decorator.shouldRenderBase(supplier))
            quads.addAll(parent.getQuads(state, side, rand, extraData, type));

        BiConsumer<BakedModel, RenderType> emitModel = (model, renderType) -> {
            if (model != null && renderType == type)
                quads.addAll(model.getQuads(state, side, rand, extraData, type));
        };

        try {
            decorator.emitQuads(supplier, emitModel);
        } catch (Exception e) {
            return quads;
        }

        return quads;
    }

    @Override
    public TextureAtlasSprite getParticleIcon (ModelData data) {
        return parent.getParticleIcon(data);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes (BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(decorator.getRenderTypes(state));
    }

    @Override
    public RenderType getRenderType (ItemStack itemStack) {
        return decorator.getRenderTypes(itemStack).get(0);
    }

    public static class PlatformDecoratedItemModel implements ItemModel
    {
        ModelResourceLocation location;
        PlatformDecoratedModel<?> parent;
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
                model = new ItemRender<>(parent, itemStack);
            }

            if (model != null) {
                ItemStackRenderState.LayerRenderState renderState = itemStackRenderState.newLayer();
                renderState.setupBlockModel(model, model.getRenderType(stack));
            }
        }

        public record Unbaked (ResourceLocation model, String variant) implements ItemModel.Unbaked {
            public static final MapCodec<PlatformDecoratedItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec((builder) ->
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

    public static class ItemRender<C extends ModelContext> extends ParentModel
    {
        PlatformDecoratedModel<C> parent;
        private ItemStack stack;

        public ItemRender (PlatformDecoratedModel<C> parent, ItemStack stack) {
            super(parent);
            this.parent = parent;
            this.stack = stack;
        }

        @Override
        public List<BakedQuad> getQuads (@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            List<BakedQuad> quads = new ArrayList<>();

            Supplier<C> supplier = () -> parent.contextSupplier.makeContext(stack);
            ModelDecorator<C> decorator = parent.decorator;
            if (decorator.shouldRenderBase(supplier, stack))
                quads.addAll(parent.getQuads(state, side, rand));

            BiConsumer<BakedModel, RenderType> emitModel = (model, renderType) -> {
                if (model != null)
                    quads.addAll(model.getQuads(state, side, rand));
            };

            try {
                decorator.emitItemQuads(supplier, emitModel, stack);
            } catch (Exception e) {
                return quads;
            }

            return quads;
        }

        @Override
        public TextureAtlasSprite getParticleIcon (ModelData data) {
            return parent.getParticleIcon(data);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes (BlockState state, RandomSource rand, ModelData data) {
            return parent.getRenderTypes(state, rand, data);
        }

        @Override
        public RenderType getRenderType (ItemStack itemStack) {
            return parent.getRenderType(itemStack);
        }
    }
}