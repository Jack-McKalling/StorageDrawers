package com.jaquadro.minecraft.storagedrawers.block.tile;

import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.DrawerModelProperties;
import com.jaquadro.minecraft.storagedrawers.capabilities.PlatformCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformBlockEntityDrawersComp extends BlockEntityDrawersComp
{
    public PlatformBlockEntityDrawersComp(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
    }

    public static class Slot2 extends BlockEntityDrawersComp.Slot2 {
        public Slot2 (BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @NotNull
        @Override
        public ModelData getModelData () {
            return DrawerModelProperties.getModelData(this);
        }

        @Override
        protected void onAttributeChanged () {
            super.onAttributeChanged();
            requestModelDataUpdate();
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability (@NotNull Capability<T> cap, @Nullable Direction side) {
            if (!PlatformCapabilities.hasCapability(cap))
                return LazyOptional.empty();

            return LazyOptional.of(() -> PlatformCapabilities.getCapability(cap, this));
        }
    }

    public static class Slot3 extends BlockEntityDrawersComp.Slot3 {
        public Slot3 (BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @NotNull
        @Override
        public ModelData getModelData () {
            return DrawerModelProperties.getModelData(this);
        }

        @Override
        protected void onAttributeChanged () {
            super.onAttributeChanged();
            requestModelDataUpdate();
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability (@NotNull Capability<T> cap, @Nullable Direction side) {
            if (!PlatformCapabilities.hasCapability(cap))
                return LazyOptional.empty();

            return LazyOptional.of(() -> PlatformCapabilities.getCapability(cap, this));
        }
    }
}
