package com.jaquadro.minecraft.storagedrawers.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ProxyBuilderModel implements BakedModel
{
    private static final List<BakedQuad> EMPTY = new ArrayList<>(0);
    private static final List<Object> EMPTY_KEY = new ArrayList<>();

    private BakedModel parent;
    private BakedModel proxy;
    private BlockState stateCache;
    private TextureAtlasSprite iconParticle;

    public ProxyBuilderModel (TextureAtlasSprite iconParticle) {
        this.iconParticle = iconParticle;
    }

    public ProxyBuilderModel (BakedModel parent) {
        this.parent = parent;
    }

    @Override
    @NotNull
    public List<BakedQuad> getQuads (BlockState state, Direction side, @NotNull RandomSource rand) {
        if (proxy == null || stateCache != state)
            setProxy(state);

        if (proxy == null)
            return EMPTY;

        return proxy.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion () {
        BakedModel model = getActiveModel();
        return model == null || model.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d () {
        BakedModel model = getActiveModel();
        return model != null && model.isGui3d();
    }

    @Override
    public boolean isCustomRenderer () {
        BakedModel model = getActiveModel();
        return model != null && model.isCustomRenderer();
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon () {
        BakedModel model = getActiveModel();
        return (model != null) ? model.getParticleIcon() : iconParticle;
    }

    @Override
    @NotNull
    public ItemTransforms getTransforms () {
        BakedModel model = getActiveModel();
        return (model != null) ? model.getTransforms() : ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    @NotNull
    public ItemOverrides getOverrides () {
        BakedModel model = getActiveModel();
        return (model != null) ? model.getOverrides() : ItemOverrides.EMPTY;
    }

    @Override
    public boolean usesBlockLight () {
        BakedModel model = getActiveModel();
        return model != null && model.usesBlockLight();
    }

    public List<Object> getKey (BlockState state) {
        return EMPTY_KEY;
    }

    protected abstract BakedModel buildModel (BlockState state, BakedModel parent);

    public final BakedModel buildModel (BlockState state) {
        return this.buildModel(state, parent);
    }

    private void setProxy (BlockState state) {
        stateCache = state;
        if (state == null)
            proxy = parent;
        else
            proxy = buildModel(state, parent);
    }

    private BakedModel getActiveModel () {
        return (proxy != null) ? proxy : parent;
    }
}
