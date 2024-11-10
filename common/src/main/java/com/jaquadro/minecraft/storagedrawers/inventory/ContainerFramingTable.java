package com.jaquadro.minecraft.storagedrawers.inventory;

import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityFramingTable;
import com.jaquadro.minecraft.storagedrawers.core.ModContainers;
import com.texelsaurus.minecraft.chameleon.inventory.content.PositionContent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContainerFramingTable extends AbstractContainerMenu
{
    private static final int InventoryX = 8;
    private static final int InventoryY = 84;
    private static final int HotbarY = 142;

    private static final int InputX = 23;
    private static final int InputY = 35;
    private static final int MaterialSideX = 50;
    private static final int MaterialSideY = 17;
    private static final int MaterialTrimX = 102;
    private static final int MaterialTrimY = 17;
    private static final int MaterialFrontX = 50;
    private static final int MaterialFrontY = 53;
    private static final int OutputX = 133;
    private static final int OutputY = 35;

    private final BlockEntityFramingTable blockEntity;
    private final Container tableInventory;
    private final Container craftResult;
    private final ContainerLevelAccess access;
    private final Player player;

    private Slot inputSlot;
    private Slot materialSideSlot;
    private Slot materialTrimSlot;
    private Slot materialFrontSlot;
    private Slot outputSlot;
    private List<Slot> playerSlots;
    private List<Slot> hotbarSlots;

    public ContainerFramingTable (int windowId, Inventory playerInv, Optional<PositionContent> content) {
        this(ModContainers.FRAMING_TABLE.get(), windowId, playerInv, PositionContent.getOrNull(content, playerInv.player.level(), BlockEntityFramingTable.class));
    }

    public ContainerFramingTable (@Nullable MenuType<?> type, int windowId, Inventory playerInventory, BlockEntityFramingTable blockEntity) {
        super(type, windowId);

        this.blockEntity = blockEntity;
        tableInventory = blockEntity.inventory();
        craftResult = blockEntity.inventory();
        access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        player = playerInventory.player;

        inputSlot = addSlot(new RestrictedSlot(tableInventory, 0, InputX, InputY));
        materialSideSlot = addSlot(new RestrictedSlot(tableInventory, 1, MaterialSideX, MaterialSideY));
        materialTrimSlot = addSlot(new RestrictedSlot(tableInventory, 2, MaterialTrimX, MaterialTrimY));
        materialFrontSlot = addSlot(new RestrictedSlot(tableInventory, 3, MaterialFrontX, MaterialFrontY));
        outputSlot = addSlot(new CraftResultSlot(playerInventory.player, tableInventory, craftResult, new int[] { 0, 1, 2, 3 }, 4, OutputX, OutputY));

        playerSlots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++)
                playerSlots.add(addSlot(new Slot(playerInventory, j + i * 9 + 9, InventoryX + j * 18, InventoryY + i * 18)));
        }

        hotbarSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++)
            hotbarSlots.add(addSlot(new Slot(playerInventory, i, InventoryX + i * 18, HotbarY)));

        slotsChanged(tableInventory);
    }

    @Override
    public ItemStack quickMoveStack (Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        // Assume inventory and hotbar slot IDs are contiguous
        int inventoryStart = playerSlots.get(0).index;
        int hotbarStart = hotbarSlots.get(0).index;
        int hotbarEnd = hotbarSlots.get(hotbarSlots.size() - 1).index + 1;

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (slotIndex == BlockEntityFramingTable.SLOT_RESULT) {
                if (!moveItemStackTo(slotStack, inventoryStart, hotbarEnd, true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(slotStack, itemStack);
            }

            else if (slotIndex == BlockEntityFramingTable.SLOT_INPUT || BlockEntityFramingTable.isMaterialSlot(slotIndex)) {
                if (!moveItemStackTo(slotStack, inventoryStart, hotbarEnd, true))
                    return ItemStack.EMPTY;
            }

            else if (slotIndex >= inventoryStart && slotIndex < hotbarEnd) {
                if (blockEntity.isItemValidTarget(slotStack)) {
                    if (!moveItemStackTo(slotStack, BlockEntityFramingTable.SLOT_INPUT, BlockEntityFramingTable.SLOT_INPUT + 1, false))
                        return ItemStack.EMPTY;
                } else if (BlockEntityFramingTable.isItemValidMaterial(slotStack)) {
                    if (!moveItemStackTo(slotStack, BlockEntityFramingTable.SLOT_SIDE, BlockEntityFramingTable.SLOT_FRONT + 1, false))
                        return ItemStack.EMPTY;
                } else if (slotIndex < hotbarStart) { // Inventory Area
                    if (!moveItemStackTo(slotStack, hotbarStart, hotbarEnd, false))
                        return ItemStack.EMPTY;
                } else { // Hotbar Area
                    if (!moveItemStackTo(slotStack, inventoryStart, hotbarStart, false))
                        return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty())
                slot.setByPlayer(ItemStack.EMPTY);
            else
                slot.setChanged();

            if (slotStack.getCount() == itemStack.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, slotStack);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid (Player player) {
        return tableInventory.stillValid(player);
    }
}