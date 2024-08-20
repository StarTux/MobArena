package com.cavetale.mobarena;

import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Mob;
import org.bukkit.util.BoundingBox;
import static com.cavetale.mobarena.MobArenaPlugin.mobArenaPlugin;
import static com.cavetale.mytems.util.Collision.collidesWithBlock;

/**
 * This is a helper class to find a location for a mob where it
 * doesn't collide with any walls.
 */
@Data
public final class MobSpawnLocation {
    private final Type type;
    private final Environment environment;
    private final List<Vec3i> options;
    private int optionShiftIndex = 1;
    private int blockShifts;
    private int mobShifts;

    public MobSpawnLocation(final Type type, final Environment environment, final List<Vec3i> options) {
        this.type = type;
        this.environment = environment;
        this.options = new ArrayList<>(options);
        Collections.shuffle(options);
    }

    public enum Type {
        MOB,
        BOSS,
        ;
    }

    public enum Environment {
        GROUND,
        FLYING,
        ;

        public static Environment of(EntityType entityType) {
            switch (entityType) {
            case ALLAY:
            case BAT:
            case BEE:
            case BLAZE:
            case ENDER_DRAGON:
            case GHAST:
            case PARROT:
            case PHANTOM:
            case VEX:
            case WITHER:
                return FLYING;
            default:
                return Flying.class.isAssignableFrom(entityType.getEntityClass())
                    ? FLYING
                    : GROUND;
            }
        }
    }

    public Location random(World world) {
        return options.get(0).toCenterFloorLocation(world);
    }

    /**
     * Spawn a mob and shift it if necessary.

     * @param world the world
     * @param callback the function that will spawn the mob
     * @return the mob or null
     */
    public Mob spawn(World world, Function<Location, Mob> callback) {
        final Location location = options.get(0).toCenterFloorLocation(world);
        final Mob mob = callback.apply(location);
        if (mob == null) {
            return null;
        }
        if (collidesWithBlock(world, mob.getBoundingBox())) {
            shiftForBlocks(mob);
        }
        if (collidesWithMob(mob, mob.getBoundingBox())) {
            shiftForMobs(mob);
        }
        return mob;
    }

    private static boolean collidesWithMob(Mob mob, BoundingBox boundingBox) {
        for (Entity entity : mob.getWorld().getNearbyEntities(boundingBox)) {
            if (entity == mob) continue;
            if (!(entity instanceof Mob)) continue;
            return true;
        }
        return false;
    }

    /**
     * Teleport a mob to a random spawn location.  Attempt to find one
     * where it doesn't collide.
     */
    public void respawn(Mob mob) {
        final World world = mob.getWorld();
        final Location location = options.get(0).toCenterFloorLocation(world);
        mob.teleport(location);
        if (collidesWithBlock(world, mob.getBoundingBox())) {
            shiftForBlocks(mob);
        }
        if (collidesWithMob(mob, mob.getBoundingBox())) {
            shiftForBlocks(mob);
        }
    }

    /**
     * Helper to shift the mob to any free spot within the remaining
     * options.
     */
    private Location shiftForBlocks(Mob mob) {
        BoundingBox bb = mob.getBoundingBox();
        final Location location = mob.getLocation();
        final World world = mob.getWorld();
        while (optionShiftIndex < options.size()) {
            blockShifts += 1;
            final Vec3i option = options.get(optionShiftIndex);
            final Location newLocation = option.toCenterFloorLocation(world);
            bb = bb.shift(newLocation.subtract(location));
            if (!collidesWithBlock(world, bb)) {
                mob.teleport(newLocation);
                return newLocation;
            }
            optionShiftIndex += 1;
        }
        mobArenaPlugin().getLogger().warning("[MobSpawnLocation] Failed to shift " + mob.getType() + " for blocks");
        return null;
    }

    /**
     * Helper to shift the mob until it no longer collides with any
     * other mob.
     */
    private Location shiftForMobs(Mob mob) {
        BoundingBox bb = mob.getBoundingBox();
        final Location location = mob.getLocation();
        final World world = mob.getWorld();
        while (optionShiftIndex < options.size()) {
            mobShifts += 1;
            final Vec3i option = options.get(optionShiftIndex);
            final Location newLocation = option.toCenterFloorLocation(world);
            bb = bb.shift(newLocation.subtract(location));
            if (!collidesWithMob(mob, bb)) {
                mob.teleport(newLocation);
                return newLocation;
            }
            optionShiftIndex += 1;
        }
        return null;
    }
}
