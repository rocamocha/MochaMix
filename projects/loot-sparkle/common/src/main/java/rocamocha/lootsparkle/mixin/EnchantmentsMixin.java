package rocamocha.lootsparkle.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.Registerable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rocamocha.lootsparkle.enchantment.EnchantmentsBootstrap;

/**
 * Mixin to register custom enchantments during bootstrap
 */
@Mixin(Enchantments.class)
public class EnchantmentsMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("LootSparkle");

    static {
        LOGGER.warn("[LootSparkle] EnchantmentsMixin class loaded");
    }

    @Inject(method = "bootstrap", at = @At("TAIL"), cancellable = false)
    private static void registerCustomEnchantments(Registerable<Enchantment> registry, CallbackInfo ci) {
        LOGGER.warn("[LootSparkle] MIXIN FIRED: Enchantments.bootstrap() called, invoking custom bootstrap");
        try {
            EnchantmentsBootstrap.bootstrap(registry);
        } catch (Exception e) {
            LOGGER.error("[LootSparkle] Exception in mixin enchantment registration", e);
        }
    }
}