package com.jaquadro.minecraft.storagedrawers.block;

import com.jaquadro.minecraft.storagedrawers.ModServices;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedSourceBlock;
import com.jaquadro.minecraft.storagedrawers.api.security.ISecurityProvider;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributesGroupControl;
import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.block.tile.util.FrameHelper;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import com.jaquadro.minecraft.storagedrawers.core.ModBlocks;
import com.jaquadro.minecraft.storagedrawers.core.ModItems;
import com.jaquadro.minecraft.storagedrawers.core.ModSecurity;
import com.jaquadro.minecraft.storagedrawers.item.ItemKeyring;
import com.jaquadro.minecraft.storagedrawers.item.ItemPersonalKey;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeRemote;
import com.jaquadro.minecraft.storagedrawers.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class BlockController extends HorizontalDirectionalBlock implements INetworked, EntityBlock, IFramedSourceBlock
{
    public BlockController (BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition (StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement (BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @NotNull
    public InteractionResult use (@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        Direction blockDir = state.getValue(FACING);
        BlockEntityController blockEntity = WorldUtils.getBlockEntity(level, pos, BlockEntityController.class);
        if (blockEntity == null)
            return InteractionResult.FAIL;

        ItemStack item = player.getInventory().getSelected();
        if (!item.isEmpty() && toggle(level, pos, player, item.getItem()))
            return InteractionResult.SUCCESS;

        if (blockDir != hit.getDirection())
            return InteractionResult.CONSUME;

        if (!level.isClientSide) {
            if (ModCommonConfig.INSTANCE.GENERAL.debugTrace.get() && item.isEmpty())
                blockEntity.printDebugInfo();

            if (item.getItem() instanceof ItemUpgradeRemote remote) {
                item = remote.setBoundController(item, blockEntity);
                player.getInventory().setItem(player.getInventory().selected, item);

                player.displayClientMessage(Component.translatable("message.storagedrawers.updated_remote_binding", pos.getX(), pos.getY(), pos.getZ()), true);
            }


            blockEntity.interactPutItemsIntoInventory(player);
        }

        return InteractionResult.SUCCESS;
    }

    public boolean toggle (Level world, BlockPos pos, Player player, Item item) {
        if (world.isClientSide || item == null)
            return false;

        if (item instanceof ItemKeyring keyring)
            item = keyring.getKey().getItem();

        if (item == ModItems.DRAWER_KEY.get())
            toggle(world, pos, player, EnumKeyType.DRAWER);
        else if (item == ModItems.SHROUD_KEY.get())
            toggle(world, pos, player, EnumKeyType.CONCEALMENT);
        else if (item == ModItems.QUANTIFY_KEY.get())
            toggle(world, pos, player, EnumKeyType.QUANTIFY);
        else if (item instanceof ItemPersonalKey itemKey)
            togglePersonal(world, pos, player, itemKey.getSecurityProviderKey());
        else
            return false;

        return true;
    }

    public void toggle (@NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull EnumKeyType keyType) {
        if (level.isClientSide)
            return;

        BlockEntityController blockEntity = WorldUtils.getBlockEntity(level, pos, BlockEntityController.class);
        if (blockEntity == null)
            return;

        IDrawerAttributesGroupControl controlAttrs = blockEntity.getGroupControllableAttributes(player);
        if (controlAttrs != null) {
            if (keyType == EnumKeyType.DRAWER)
                controlAttrs.toggleItemLocked(EnumSet.allOf(LockAttribute.class), LockAttribute.LOCK_POPULATED);
            else if (keyType == EnumKeyType.CONCEALMENT)
                controlAttrs.toggleConcealed();
            else if (keyType == EnumKeyType.QUANTIFY)
                controlAttrs.toggleIsShowingQuantity();
        }
    }

    public void togglePersonal (@NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, String providerKey) {
        if (level.isClientSide)
            return;

        BlockEntityController blockEntity = WorldUtils.getBlockEntity(level, pos, BlockEntityController.class);
        if (blockEntity == null)
            return;

        ISecurityProvider provider = ModSecurity.registry.getProvider(providerKey);
        blockEntity.toggleProtection(player.getGameProfile(), provider);
    }

    @Override
    public void tick (@NotNull BlockState state, @NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        if (world.isClientSide)
            return;

        BlockEntityController blockEntity = WorldUtils.getBlockEntity(world, pos, BlockEntityController.class);
        if (blockEntity == null)
            return;

        blockEntity.updateCache();

        world.scheduleTick(pos, this, 100);
    }

    @Override
    public BlockEntityController newBlockEntity (@NotNull BlockPos pos, @NotNull BlockState state) {
        return ModServices.RESOURCE_FACTORY.createBlockEntityController().create(pos, state);
    }

    @Override
    public ItemStack makeFramedItem (ItemStack source, ItemStack matSide, ItemStack matTrim, ItemStack matFront) {
        return FrameHelper.makeFramedItem(ModBlocks.FRAMED_CONTROLLER.get(), source, matSide, matTrim, matFront);
    }
}
