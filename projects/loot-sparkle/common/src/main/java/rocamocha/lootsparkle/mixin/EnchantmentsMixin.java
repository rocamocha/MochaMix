package rocamocha.lootsparkle.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.Registerable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import rocamocha.lootsparkle.enchantment.EnchantmentsBootstrap;

/**
 * Mixin to register custom enchantments during bootstrap
 */
@Mixin(Enchantments.class)
public class EnchantmentsMixin {

    @Inject(method = "bootstrap", at = @At("TAIL"), cancellable = false)
    private static void registerCustomEnchantments(Registerable<Enchantment> registry, CallbackInfo ci) {
        try {
            EnchantmentsBootstrap.bootstrap(registry);
        } catch (Exception e) {
        }
    }
}
