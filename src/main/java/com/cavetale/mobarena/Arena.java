package com.cavetale.mobarena;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.winthier.creative.BuildWorld;
import java.io.Serializable;
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
public final class Arena implements Serializable {
    private String buildWorldPath;
    private String buildWorldName;
    private String worldName;
    private Cuboid arenaArea;
    /**
     * This list and 3 others will be created from a set in the
     * conctructor.  They will be purged based on actual blocks later,
     * when we know that the chunks are loaded.
     */
    private List<Vec3i> spawnVectorList;
    private List<Vec3i> mobVectorList;
    private List<Vec3i> flyingMobVectorList;
    private Vec3i bossChestVector;
    private List<Cuboid> forbiddenList = new ArrayList<>();
    private List<Cuboid> bossEscapeList = new ArrayList<>();

    public Arena() { }

    public Arena(final World world, final BuildWorld buildWorld) {
        this.buildWorldPath = buildWorld.getPath();
        this.buildWorldName = buildWorld.getName();
        this.worldName = world.getName();
        // Here we load the areas file, which is still in
        // the old format which assumes multiple arenas
        // per world.  So we just pick the first area list
        // from the map and feed that into the Arena, for
        // now.
        final AreasFile areasFile = AreasFile.load(world, "MobArena");
        final List<Area> areaList = areasFile.getAreas().values().iterator().next();
        if (areaList.isEmpty()) {
            throw new IllegalArgumentException("areaList cannot be empty!");
        }
        this.arenaArea = areaList.get(0).toCuboid();
        Set<Vec3i> spawn = new HashSet<>();
        Set<Vec3i> mob = new HashSet<>();
        Set<Vec3i> flyingmob = new HashSet<>();
        Vec3i bosschest = null;
        for (Area area : areaList) {
            if (area.name == null) continue;
            switch (area.name) {
            case "spawn":
                spawn.addAll(area.toCuboid().enumerate());
                break;
            case "mob":
            case "mobs":
                mob.addAll(area.toCuboid().enumerate());
                break;
            case "flyingmob":
            case "flyingmobs":
                flyingmob.addAll(area.toCuboid().enumerate());
                break;
            case "bosschest":
                bosschest = area.min;
                break;
            case "forbidden":
                forbiddenList.add(area.toCuboid());
                break;
            case "bossescape":
                bossEscapeList.add(area.toCuboid());
                break;
            default:
                MobArenaPlugin.instance.getLogger().warning("Arena " + buildWorldPath + ": Unknown area: " + area);
            }
        }
        this.spawnVectorList = new ArrayList<>(spawn);
        this.mobVectorList = new ArrayList<>(mob);
        this.flyingMobVectorList = new ArrayList<>(flyingmob);
        this.bossChestVector = bosschest;
        if (spawnVectorList.isEmpty()) {
            MobArenaPlugin.instance.getLogger().severe("Arena " + buildWorldPath + ": No spawns!");
        }
        if (mobVectorList.isEmpty()) {
            MobArenaPlugin.instance.getLogger().severe("Arena " + buildWorldPath + ": No mobs!");
        }
        if (flyingMobVectorList.isEmpty()) {
            MobArenaPlugin.instance.getLogger().warning("Arena " + buildWorldPath + ": No flying mobs!");
        }
        if (bossChestVector == null) {
            MobArenaPlugin.instance.getLogger().severe("Arena " + buildWorldPath + ": No boss chest!");
        }
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
        return randomVector(spawnVectorList).toCenterFloorLocation(getWorld());
    }

    public Location randomMobLocation() {
        return randomVector(mobVectorList).toCenterFloorLocation(getWorld());
    }

    public Location randomFlyingMobLocation() {
        if (flyingMobVectorList.isEmpty()) return randomMobLocation();
        return randomVector(flyingMobVectorList).toCenterFloorLocation(getWorld());
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
            result.add(player);
        }
        return result;
    }

    public boolean isOnPlane(Location location) {
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return arenaArea.ax <= x && x <= arenaArea.bx
            && arenaArea.az <= z && z <= arenaArea.bz;
    }

    public boolean isInWorld(Location location) {
        return worldName.equals(location.getWorld().getName());
    }

    public boolean isInWorld(World world) {
        return worldName.equals(world.getName());
    }

    public boolean isForbidden(Location location) {
        for (Cuboid forbidden : forbiddenList) {
            if (forbidden.contains(location)) return true;
        }
        return false;
    }

    public boolean isBossEscape(Location location) {
        for (Cuboid bossEscape : bossEscapeList) {
            if (bossEscape.contains(location)) return true;
        }
        return false;
    }
}
