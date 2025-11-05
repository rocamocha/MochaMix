package rocamocha.lootsparkle.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocamocha.lootsparkle.enchantment.EnchantmentRestrictionHandler;

@Mixin(Enchantment.class)
public abstract class EnchantmentRestrictionMixin {
    
    @Shadow
    public abstract Text description();
    
    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void enforceCustomRestrictions(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        String desc = this.description().getString();
        
        if (desc.contains("Soul Sight")) {
            if (!EnchantmentRestrictionHandler.isValidSoulSightItem(stack)) {
                cir.setReturnValue(false);
            }
        } else if (desc.contains("Fairy Dust")) {
            if (!EnchantmentRestrictionHandler.isValidFairyDustItem(stack)) {
                cir.setReturnValue(false);
            }
        }
    }
}
