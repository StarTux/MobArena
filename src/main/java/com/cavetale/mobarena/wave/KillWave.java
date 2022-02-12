package com.cavetale.mobarena.wave;

import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.LivingEnemyWrapper;
import com.cavetale.enemy.util.ItemBuilder;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.KillWaveTag;
import com.cavetale.mobarena.state.GameState;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import static java.awt.Color.HSBtoRGB;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class KillWave extends Wave<KillWaveTag> {
    protected static final Map<EntityType, Integer> ENTITY_MIN_WAVE_MAP = new EnumMap<>(EntityType.class);
    protected Color leatherArmorColor;

    protected KillWave(final Game game) {
        super(game, WaveType.KILL, KillWaveTag.class, KillWaveTag::new);
    }

    static {
        //ENTITY_MIN_WAVE_MAP.put(EntityType.ENDERMITE, idx);
        //ENTITY_MIN_WAVE_MAP.put(EntityType.SILVERFISH, idx);
        int idx = 0;
        ENTITY_MIN_WAVE_MAP.put(EntityType.CREEPER, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ZOMBIE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.SKELETON, idx);
        idx = 10;
        ENTITY_MIN_WAVE_MAP.put(EntityType.DROWNED, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.HUSK, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.SLIME, idx);
        idx = 20;
        ENTITY_MIN_WAVE_MAP.put(EntityType.BLAZE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.MAGMA_CUBE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.PIGLIN, idx);
        idx = 30;
        ENTITY_MIN_WAVE_MAP.put(EntityType.HOGLIN, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.WITHER_SKELETON, idx);
        idx = 40;
        ENTITY_MIN_WAVE_MAP.put(EntityType.PHANTOM, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.WITCH, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ZOGLIN, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ZOMBIFIED_PIGLIN, idx);
        idx = 50;
        ENTITY_MIN_WAVE_MAP.put(EntityType.PIGLIN_BRUTE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.PILLAGER, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.GHAST, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.VINDICATOR, idx);
        idx = 60;
        ENTITY_MIN_WAVE_MAP.put(EntityType.EVOKER, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.RAVAGER, idx);
    }

    @Override
    public WaveType getWaveType() {
        return WaveType.KILL;
    }

    @Override
    public void create() {
        int mobCount = game.getTag().getCurrentWaveIndex() + game.countActivePlayers();
        List<EntityType> entityTypeList = new ArrayList<>(ENTITY_MIN_WAVE_MAP.keySet());
        entityTypeList.removeIf(et -> ENTITY_MIN_WAVE_MAP.get(et) > game.getTag().getCurrentWaveIndex());
        Collections.shuffle(entityTypeList);
        int mobTypeCount = (mobCount - 1) / 10 + 1;
        entityTypeList = List.copyOf(entityTypeList.subList(0, Math.min(entityTypeList.size(), mobTypeCount)));
        for (int i = 0; i < mobCount; i += 1) {
            EntityType entityType = entityTypeList.get(i % entityTypeList.size());
            KillWaveTag.MobSpawn mobSpawn = new KillWaveTag.MobSpawn();
            mobSpawn.setEntityType(entityType);
            tag.getMobSpawnList().add(mobSpawn);
        }
        tag.setTotalMobCount(mobCount);
        tag.setStillAlive(mobCount);
        displayName = text("Kill All Mobs", BLUE);
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
                Mob mob = spawnMob(mobSpawn);
                enemy = new LivingEnemyWrapper(game.getEnemyContext(), mob);
                game.getEnemies().add(enemy);
                mobSpawn.setEnemyId(enemy.getEnemyId());
            } else {
                if (!game.getActivePlayers().contains(enemy.getCurrentTarget())) {
                    enemy.findPlayerTarget();
                }
            }
        }
        tag.setStillAlive(stillAlive);
        if (stillAlive == 0) finished = true;
    }

    protected Mob spawnMob(KillWaveTag.MobSpawn mobSpawn) {
        Class<? extends Entity> entityClass = mobSpawn.getEntityType().getEntityClass();
        if (!Mob.class.isAssignableFrom(entityClass)) {
            mobSpawn.setDead(true);
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Mob> livingEntityClass = (Class<? extends Mob>) entityClass;
        boolean flying = Flying.class.isAssignableFrom(livingEntityClass);
        Location location = flying
            ? game.getArena().randomFlyingMobLocation()
            : game.getArena().randomMobLocation();
        Mob mob = location.getWorld().spawn(location, livingEntityClass, false, e -> {
                e.setPersistent(false);
                e.setRemoveWhenFarAway(false);
                if (e instanceof Slime slime) {
                    slime.setSize(3);
                }
            });
        final int difficultyLevel = game.getTag().getCurrentWaveIndex() / 10;
        if (mobSpawn.getEntityType() == EntityType.ZOMBIE) {
            mob.getEquipment().setHelmet(Mytems.KOBOLD_HEAD.createItemStack());
        }
        if (mob instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
            equipHumanoid(mob, difficultyLevel);
        } else if (mob instanceof Skeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
            equipHumanoid(mob, difficultyLevel);
        } else if (mob instanceof PiglinAbstract piglin) {
            piglin.setImmuneToZombification(true);
            if (piglin instanceof Piglin piglin2) {
                piglin2.setIsAbleToHunt(false);
                for (Material mat : List.copyOf(piglin2.getBarterList())) {
                    piglin2.removeBarterMaterial(mat);
                }
                for (Material mat : List.copyOf(piglin2.getInterestList())) {
                    piglin2.removeMaterialOfInterest(mat);
                }
            }
        } else if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
            hoglin.setIsAbleToBeHunted(false);
        }
        adjustAttributes(mob, difficultyLevel);
        return mob;
    }

    protected void equipHumanoid(Mob mob, int difficultyLevel) {
        List<Material> helmets = List.of(Material.LEATHER_HELMET,
                                         Material.GOLDEN_HELMET,
                                         Material.IRON_HELMET,
                                         Material.CHAINMAIL_HELMET,
                                         Material.DIAMOND_HELMET,
                                         Material.NETHERITE_HELMET);
        List<Material> chestplates = List.of(Material.LEATHER_CHESTPLATE,
                                             Material.GOLDEN_CHESTPLATE,
                                             Material.IRON_CHESTPLATE,
                                             Material.CHAINMAIL_CHESTPLATE,
                                             Material.DIAMOND_CHESTPLATE,
                                             Material.NETHERITE_CHESTPLATE);
        List<Material> leggings = List.of(Material.LEATHER_LEGGINGS,
                                          Material.GOLDEN_LEGGINGS,
                                          Material.IRON_LEGGINGS,
                                          Material.CHAINMAIL_LEGGINGS,
                                          Material.DIAMOND_LEGGINGS,
                                          Material.NETHERITE_LEGGINGS);
        List<Material> boots = List.of(Material.LEATHER_BOOTS,
                                       Material.GOLDEN_BOOTS,
                                       Material.IRON_BOOTS,
                                       Material.CHAINMAIL_BOOTS,
                                       Material.DIAMOND_BOOTS,
                                       Material.NETHERITE_BOOTS);
        List<Material> swords = List.of(Material.WOODEN_SWORD,
                                        Material.STONE_SWORD,
                                        Material.GOLDEN_SWORD,
                                        Material.IRON_SWORD,
                                        Material.DIAMOND_SWORD,
                                        Material.NETHERITE_SWORD);
        List<Material> axes = List.of(Material.WOODEN_AXE,
                                      Material.STONE_AXE,
                                      Material.GOLDEN_AXE,
                                      Material.IRON_AXE,
                                      Material.DIAMOND_AXE,
                                      Material.NETHERITE_AXE);
        List<Material> mats = new ArrayList<>();
        mats.add(helmets.get(Math.min(difficultyLevel, helmets.size() - 1)));
        mats.add(chestplates.get(Math.min(difficultyLevel, chestplates.size() - 1)));
        mats.add(leggings.get(Math.min(difficultyLevel, leggings.size() - 1)));
        mats.add(boots.get(Math.min(difficultyLevel, boots.size() - 1)));
        mats.add(game.getRandom().nextBoolean()
                 ? swords.get(Math.min(difficultyLevel, swords.size() - 1))
                 : axes.get(Math.min(difficultyLevel, axes.size() - 1)));
        mats.add(difficultyLevel < 3 ? null : Material.SHIELD);
        List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD,
                                            EquipmentSlot.CHEST,
                                            EquipmentSlot.LEGS,
                                            EquipmentSlot.FEET,
                                            EquipmentSlot.HAND,
                                            EquipmentSlot.OFF_HAND);
        EntityEquipment eq = mob.getEquipment();
        for (int i = 0; i < slots.size(); i += 1) {
            EquipmentSlot slot = slots.get(i);
            eq.setDropChance(slot, 0.0f);
            ItemStack oldItem = eq.getItem(slot);
            if (oldItem != null && oldItem.getType() != Material.AIR) continue;
            Material mat = mats.get(i);
            if (mat == null) continue;
            ItemStack itemStack = new ItemBuilder(mat).removeArmor().removeDamage().create();
            itemStack.editMeta(m -> {
                    if (m instanceof LeatherArmorMeta meta) {
                        if (leatherArmorColor == null) {
                            leatherArmorColor = Color.fromRGB(0xFFFFFF & HSBtoRGB(game.getRandom().nextFloat(), 1.0f, 1.0f));
                        }
                        meta.setColor(leatherArmorColor);
                    }
                });
            eq.setItem(slot, itemStack);
        }
    }

    protected void adjustAttributes(Mob mob, int difficultyLevel) {
        Attribute attribute = null;
        AttributeInstance inst = null;
        double value = 0.0;
        try {
            attribute = Attribute.GENERIC_ARMOR;
            inst = mob.getAttribute(attribute);
            if (inst != null) {
                value = inst.getBaseValue() + 7.0f + 3 * (double) difficultyLevel;
                mob.getAttribute(attribute).setBaseValue(value);
            }
            attribute = Attribute.GENERIC_ARMOR_TOUGHNESS;
            inst = mob.getAttribute(attribute);
            if (inst != null) {
                value = inst.getBaseValue() + 2.0 * (double) difficultyLevel;
                mob.getAttribute(attribute).setBaseValue(value);
            }
            attribute = Attribute.GENERIC_ATTACK_DAMAGE;
            inst = mob.getAttribute(attribute);
            if (inst != null) {
                value = inst.getBaseValue() + (double) difficultyLevel;
                mob.getAttribute(attribute).setBaseValue(value);
            }
            attribute = Attribute.GENERIC_MAX_HEALTH;
            inst = mob.getAttribute(attribute);
            if (inst != null) {
                value = inst.getBaseValue() + 2.0 * (double) difficultyLevel;
                mob.getAttribute(attribute).setBaseValue(value);
                mob.setHealth(value);
            }
        } catch (Exception e) {
            game.getPlugin().getLogger().log(Level.WARNING, mob.getType() + " " + attribute + " " + value, e);
        }
    }

    @Override
    public void end() {
        game.clearEnemies();
        game.clearTemporaryEntities();
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
        bossBar.color(BossBar.Color.BLUE);
        bossBar.overlay(BossBar.Overlay.PROGRESS);
        int alive = tag.getStillAlive();
        int total = tag.getTotalMobCount();
        bossBar.name(text("Kill All Mobs", BLUE));
        float progress = total > 0 ? (float) alive / (float) total : 0f;
        bossBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> lines) {
        lines.add(join(noSeparators(),
                       text("Mobs ", GRAY),
                       text(tag.getStillAlive(), GREEN)));
    }
}
