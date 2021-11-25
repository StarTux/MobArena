package com.cavetale.mobarena.wave;

import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.LivingEnemyWrapper;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.KillWaveTag;
import com.cavetale.mobarena.state.GameState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public final class KillWave extends Wave<KillWaveTag> {
    protected static final Map<EntityType, Integer> ENTITY_MIN_WAVE_MAP = new EnumMap<>(EntityType.class);

    protected KillWave(final Game game) {
        super(game, WaveType.KILL, KillWaveTag.class, KillWaveTag::new);
    }

    static {
        int idx = 0;
        ENTITY_MIN_WAVE_MAP.put(EntityType.CREEPER, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ZOMBIE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ZOMBIE_VILLAGER, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.SKELETON, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.CREEPER, idx);
        idx = 10;
        ENTITY_MIN_WAVE_MAP.put(EntityType.BLAZE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.DROWNED, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.HUSK, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ENDERMITE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.SILVERFISH, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.MAGMA_CUBE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.SLIME, idx);
        idx = 20;
        ENTITY_MIN_WAVE_MAP.put(EntityType.PHANTOM, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.WITCH, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.WITHER_SKELETON, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ZOGLIN, idx);
        idx = 30;
        ENTITY_MIN_WAVE_MAP.put(EntityType.GHAST, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.PIGLIN_BRUTE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.PILLAGER, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.VINDICATOR, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.EVOKER, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.RAVAGER, idx);
    }

    @Override
    public WaveType getWaveType() {
        return WaveType.KILL;
    }

    @Override
    public void create() {
        int mobCount = game.getTag().getCurrentWaveIndex() + 10;
        List<EntityType> entityTypeList = new ArrayList<>(ENTITY_MIN_WAVE_MAP.keySet());
        entityTypeList.removeIf(et -> ENTITY_MIN_WAVE_MAP.get(et) > game.getTag().getCurrentWaveIndex());
        Collections.shuffle(entityTypeList);
        int mobTypeCount = (mobCount - 1) / 10 + 1;
        entityTypeList = List.copyOf(entityTypeList.subList(0, mobTypeCount));
        for (int i = 0; i < mobCount; i += 1) {
            EntityType entityType = entityTypeList.get(i % entityTypeList.size());
            KillWaveTag.MobSpawn mobSpawn = new KillWaveTag.MobSpawn();
            mobSpawn.setEntityType(entityType);
            tag.getMobSpawnList().add(mobSpawn);
        }
        tag.setTotalMobCount(mobCount);
        tag.setStillAlive(mobCount);
        displayName = Component.text("Kill " + mobCount + " mobs", NamedTextColor.DARK_RED);
    }

    @Override
    public void start() { }

    @Override
    public void tick() {
        int stillAlive = 0;
        for (KillWaveTag.MobSpawn mobSpawn : tag.getMobSpawnList()) {
            if (mobSpawn.isDead()) continue;
            stillAlive += 1;
            Enemy enemy = Enemy.ofEnemyId(mobSpawn.getEnemyId());
            if (enemy == null || !enemy.isValid()) {
                spawnMob(mobSpawn);
            } else {
                if (!game.getActivePlayers().contains(enemy.getCurrentTarget())) {
                    enemy.findPlayerTarget();
                }
            }
        }
        tag.setStillAlive(stillAlive);
        if (stillAlive == 0) finished = true;
    }

    protected void spawnMob(KillWaveTag.MobSpawn mobSpawn) {
        Class<? extends Entity> entityClass = mobSpawn.getEntityType().getEntityClass();
        if (!LivingEntity.class.isAssignableFrom(entityClass)) {
            mobSpawn.setDead(true);
            return;
        }
        @SuppressWarnings("unchecked")
        Class<? extends LivingEntity> livingEntityClass = (Class<? extends LivingEntity>) entityClass;
        boolean flying = Flying.class.isAssignableFrom(livingEntityClass);
        Location location = flying
            ? game.getArena().randomFlyingMobLocation()
            : game.getArena().randomMobLocation();
        LivingEntity entity = location.getWorld().spawn(location, livingEntityClass, e -> {
                e.setPersistent(false);
                e.setRemoveWhenFarAway(false);
                e.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
            });
        Enemy enemy = new LivingEnemyWrapper(game.getEnemyContext(), (LivingEntity) entity);
        game.getEnemies().add(enemy);
        mobSpawn.setEnemyId(enemy.getEnemyId());
    }

    @Override
    public void end() {
        game.clearEnemies();
    }

    @Override
    public void onDeath(Enemy enemy) {
        for (KillWaveTag.MobSpawn mobSpawn : tag.getMobSpawnList()) {
            if (mobSpawn.getEnemyId() == enemy.getEnemyId()) {
                mobSpawn.setDead(true);
            }
        }
    }

    /**
     * When this is loaded from disk, add all the enemies to the game.
     */
    @Override
    public void onLoad() {
        if (game.getTag().getState() == GameState.WAVE) {
            for (KillWaveTag.MobSpawn mobSpawn : tag.getMobSpawnList()) {
                if (mobSpawn.isDead()) continue;
                Enemy enemy = Enemy.ofEnemyId(mobSpawn.getEnemyId());
                if (enemy != null) {
                    game.getEnemies().add(enemy);
                    enemy.setContext(game.getEnemyContext());
                }
            }
        }
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        bossBar.color(BossBar.Color.RED);
        bossBar.overlay(BossBar.Overlay.PROGRESS);
        int alive = tag.getStillAlive();
        int total = tag.getTotalMobCount();
        bossBar.name(Component.text("" + alive + "/" + total + " Enemies", NamedTextColor.DARK_RED));
        float progress = total > 0 ? (float) alive / (float) total : 0f;
        bossBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));
    }
}
