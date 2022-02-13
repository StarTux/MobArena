package com.cavetale.mobarena;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Runtime instance of an arena configuration.
 */
@Data
public final class Arena {
    protected final String name;
    protected final String worldName;
    protected final Cuboid arenaArea;
    /**
     * This list and 3 others will be created from a set in the
     * conctructor.  They will be purged based on actual blocks later,
     * when we know that the chunks are loaded.
     */
    protected final List<Vec3i> spawnVectorList;
    protected final List<Vec3i> mobVectorList;
    protected final List<Vec3i> flyingMobVectorList;
    protected final Vec3i bossChestVector;

    public Arena(final World world, final String name, final List<Cuboid> areaList) {
        this.name = name;
        this.worldName = world.getName();
        if (areaList.isEmpty()) {
            throw new IllegalArgumentException("areaList cannot be empty!");
        }
        this.arenaArea = areaList.get(0);
        Set<Vec3i> spawn = new HashSet<>();
        Set<Vec3i> mob = new HashSet<>();
        Set<Vec3i> flyingmob = new HashSet<>();
        Vec3i bosschest = null;
        for (Cuboid area : areaList) {
            if (area.name == null) continue;
            switch (area.name) {
            case "spawn":
                spawn.addAll(area.enumerate());
                break;
            case "mob":
            case "mobs":
                mob.addAll(area.enumerate());
                break;
            case "flyingmob":
            case "flyingmobs":
                flyingmob.addAll(area.enumerate());
                break;
            case "bosschest":
                bosschest = area.min;
                break;
            default: break;
            }
        }
        this.spawnVectorList = new ArrayList<>(spawn);
        this.mobVectorList = new ArrayList<>(mob);
        this.flyingMobVectorList = new ArrayList<>(flyingmob);
        this.bossChestVector = bosschest;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public void purge() {
        World world = getWorld();
        if (world == null) return;
        spawnVectorList.removeIf(vector -> !vector.toBlock(world).getCollisionShape().getBoundingBoxes().isEmpty());
        mobVectorList.removeIf(vector -> {
                return !vector.toBlock(world).getCollisionShape().getBoundingBoxes().isEmpty()
                    || vector.add(0, -1, 0).toBlock(world).getCollisionShape().getBoundingBoxes().isEmpty();
            });
        flyingMobVectorList.removeIf(vector -> !vector.toBlock(world).isEmpty());
    }

    private static Vec3i randomVector(List<Vec3i> vectorList) {
        Random random = ThreadLocalRandom.current();
        return vectorList.get(random.nextInt(vectorList.size()));
    }

    public Location randomSpawnLocation() {
        return randomVector(spawnVectorList).toLocation(getWorld());
    }

    public Location randomMobLocation() {
        return randomVector(mobVectorList).toLocation(getWorld());
    }

    public Location randomFlyingMobLocation() {
        if (flyingMobVectorList.isEmpty()) return randomMobLocation();
        return randomVector(flyingMobVectorList).toLocation(getWorld());
    }

    public Block bossChestBlock() {
        return bossChestVector.toBlock(getWorld());
    }

    public boolean isInArena(Location location) {
        return arenaArea.contains(location);
    }

    public List<Player> getPresentPlayers() {
        World world = getWorld();
        if (world == null) return List.of();
        List<Player> result = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (isInArena(player.getLocation())) {
                result.add(player);
            }
        }
        return result;
    }
}
