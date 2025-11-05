package rocamocha.lootsparkle.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import rocamocha.lootsparkle.sparkle.Sparkle;

/**
 * Screen handler for sparkle inventories
 *
 * Manages the interaction between player inventory and sparkle inventory
 */
public class SparkleScreenHandler extends net.minecraft.screen.GenericContainerScreenHandler {
    private final SimpleInventory sparkleInventory;

    public SparkleScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory sparkleInventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, sparkleInventory, 3); // 3 rows for 27 slots
        this.sparkleInventory = sparkleInventory;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.sparkleInventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);

        if (slot2 != null && slot2.hasStack()) {
            ItemStack itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();

            // If the slot is in the sparkle inventory (0-26)
            if (slot < 27) {
                // Try to move to player inventory/hotbar
                if (!this.insertItem(itemStack2, 27, 63, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try to move to sparkle inventory
                if (!this.insertItem(itemStack2, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }

        return itemStack;
    }

    public SimpleInventory getSparkleInventory() {
        return sparkleInventory;
    }

    /**
     * Factory for creating sparkle screen handlers
     */
    public static class Factory implements NamedScreenHandlerFactory {
        private final Sparkle sparkle;

        public Factory(Sparkle sparkle) {
            this.sparkle = sparkle;
        }

        @Override
        public Text getDisplayName() {
            return Text.literal("Sparkle");
        }

        @Override
        public SparkleScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new SparkleScreenHandler(syncId, playerInventory, sparkle.getInventory());
        }
    }
}
