package rocamocha.lootsparkle.trial;

import rocamocha.lootsparkle.sparkle.Sparkle;

/**
 * Custom FallingBlockEntity that can be damaged by projectiles.
 * Used for target entities in challenge phases that need to accept arrow hits.
 */
public class TargetFallingBlockEntity extends net.minecraft.entity.FallingBlockEntity {
    private final Sparkle sparkle;
    private float time = 0.0f;
    private final double baseX, baseY, baseZ;
    
    public TargetFallingBlockEntity(Sparkle sparkle, net.minecraft.world.World world, double x, double y, double z, net.minecraft.block.BlockState block) {
        super(net.minecraft.entity.EntityType.FALLING_BLOCK, world);
        this.sparkle = sparkle;
        this.baseX = x;
        this.baseY = y;
        this.baseZ = z;
        this.intersectionChecked = true;
        this.setPosition(x, y, z);
        this.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.setFallingBlockPos(this.getBlockPos());
        
        // Try multiple approaches to set the block state
        boolean success = false;
        
        // Method 1: Try common field names across different mappings
        String[] possibleFieldNames = {"block", "field_145850_b", "blockState", "f_31950_"};
        for (String fieldName : possibleFieldNames) {
            try {
                java.lang.reflect.Field blockField = net.minecraft.entity.FallingBlockEntity.class.getDeclaredField(fieldName);
                blockField.setAccessible(true);
                blockField.set(this, block);
                success = true;
                break;
            } catch (Exception e) {
                // Try next field name
            }
        }
        
        // Method 2: If all field names fail, search for the field by type
        if (!success) {
            try {
                for (java.lang.reflect.Field field : net.minecraft.entity.FallingBlockEntity.class.getDeclaredFields()) {
                    if (field.getType() == net.minecraft.block.BlockState.class) {
                        field.setAccessible(true);
                        field.set(this, block);
                        success = true;
                        break;
                    }
                }
            } catch (Exception e) {
                // Log the error since we couldn't set the block
                System.err.println("Failed to set block state for TargetFallingBlockEntity - targets will appear as sand!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        // Allow projectile damage to succeed so arrows don't deflect
        if (source.getType().msgId().contains("arrow") || source.getType().msgId().contains("projectile")) {
            // Notify the sparkle that this target was hit
            sparkle.onTargetHit(this, source, this.getWorld());
            return true;
        }
        return super.damage(source, amount);
    }

    @Override
    public void tick() {
        // Prevent the entity from timing out and being discarded after 30 seconds
        try {
            java.lang.reflect.Field timeField = net.minecraft.entity.FallingBlockEntity.class.getDeclaredField("timeFalling");
            timeField.setAccessible(true);
            timeField.setInt(this, 0);
        } catch (Exception e) {
        }
        super.tick();
        time += 0.15f;
        double offsetY = Math.sin(time) * 0.15;
        double offsetX = Math.sin(time * 0.3) * 0.1;
        double offsetZ = Math.cos(time * 0.3) * 0.1;
        this.setPosition(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
        this.setVelocity(0, 0, 0);
    }
}
