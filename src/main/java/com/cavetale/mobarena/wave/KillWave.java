package com.cavetale.mobarena.wave;

import com.cavetale.core.font.Unicode;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.LivingEnemy;
import com.cavetale.enemy.LivingEnemyWrapper;
import com.cavetale.enemy.util.ItemBuilder;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.KillWaveTag;
import com.cavetale.mobarena.state.GameState;
import com.cavetale.mobarena.util.Time;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.event.combat.DamageCalculationEvent;
import com.cavetale.mytems.util.Entities;
import com.cavetale.mytems.util.Skull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import static java.awt.Color.HSBtoRGB;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class KillWave extends Wave<KillWaveTag> {
    protected static final Map<EntityType, Integer> ENTITY_MIN_WAVE_MAP = new EnumMap<>(EntityType.class);
    protected static final Map<EntityType, Integer> ENTITY_WEIGHT_MAP = new EnumMap<>(EntityType.class);
    protected static final Skull SKELETON_SKULL =
        new Skull("Skeleton",
                  UUID.fromString("aa84e219-41f5-4a9f-a4e0-49e003aa29e8"),
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjY"
                  + "wOTMyYjJjMjI0YjdjMjE2M2NiYWMwNWYyODZhMDZlNjkyMjUyN2IwMzY2N2M0ZTk2ZjcyOGU1YzRmZmI2NiJ9fX0=",
                  null);
    protected Color leatherArmorColor;
    protected long lastWarpHome;
    protected Duration runningTime = Duration.ofSeconds(0);

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
        ENTITY_MIN_WAVE_MAP.put(EntityType.SPIDER, idx);
        idx = 10;
        ENTITY_MIN_WAVE_MAP.put(EntityType.DROWNED, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.HUSK, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.STRAY, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.SLIME, idx);
        idx = 20;
        ENTITY_MIN_WAVE_MAP.put(EntityType.BLAZE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.MAGMA_CUBE, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.PIGLIN, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.ENDERMAN, idx);
        idx = 30;
        ENTITY_MIN_WAVE_MAP.put(EntityType.HOGLIN, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.WITHER_SKELETON, idx);
        ENTITY_MIN_WAVE_MAP.put(EntityType.CAVE_SPIDER, idx);
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
        ENTITY_MIN_WAVE_MAP.put(EntityType.ILLUSIONER, idx);
        // Weights
        for (EntityType entityType : ENTITY_MIN_WAVE_MAP.keySet()) {
            ENTITY_WEIGHT_MAP.put(entityType, 3);
        }
        ENTITY_WEIGHT_MAP.put(EntityType.ENDERMAN, 1);
    }

    @Override
    public WaveType getWaveType() {
        return WaveType.KILL;
    }

    @Override
    public void create() {
        final int wave = getEffectiveWave();
        final int mobCount = Math.max(8, (wave / 5) + (2 * game.countActivePlayers()));
        List<EntityType> entityTypeList = new ArrayList<>();
        for (EntityType entityType : ENTITY_MIN_WAVE_MAP.keySet()) {
            int min = ENTITY_MIN_WAVE_MAP.get(entityType);
            if (min <= wave) {
                int weight = ENTITY_WEIGHT_MAP.get(entityType);
                for (int i = 0; i < weight; i += 1) {
                    entityTypeList.add(entityType);
                }
            }
        }
        assert !entityTypeList.isEmpty();
        final int mobTypeCount = Math.min(entityTypeList.size(), ((mobCount - 1) / 16) + 1);
        Collections.shuffle(entityTypeList);
        for (int i = 0; i < mobCount; i += 1) {
            EntityType entityType = entityTypeList.get(i % mobTypeCount);
            KillWaveTag.MobSpawn mobSpawn = new KillWaveTag.MobSpawn();
            mobSpawn.setEntityType(entityType);
            tag.getMobSpawnList().add(mobSpawn);
        }
        tag.setTotalMobCount(mobCount);
        tag.setStillAlive(mobCount);
    }

    @Override
    public void start() { }

    @Override
    public void tick() {
        int stillAlive = 0;
        runningTime = Duration.ofMillis(System.currentTimeMillis() - game.getStateHandler().getTag().getStartTime());
        boolean doWarpHome = (runningTime.toSeconds() % 90L) == 0L && lastWarpHome != runningTime.toSeconds();
        if (doWarpHome) {
            lastWarpHome = runningTime.toSeconds();
        }
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
                if (game.getArena().isForbidden(enemy.getLocation())) {
                    doWarpHome = true;
                }
                if (doWarpHome) {
                    LivingEntity livingEntity = enemy instanceof LivingEnemy livingEnemy
                        ? livingEnemy.getLivingEntity()
                        : null;
                    boolean flying = livingEntity != null
                        && livingEntity instanceof Flying;
                    Location location = flying
                        ? game.getArena().randomFlyingMobLocation()
                        : game.getArena().randomMobLocation();
                    enemy.teleport(location);
                    if (livingEntity != null) {
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, true, false, false));
                    }
                } else if (!game.getActivePlayers().contains(enemy.getCurrentTarget())) {
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
        return location.getWorld().spawn(location, livingEntityClass, false, this::spawnMobCallback);
    }

    protected void spawnMobCallback(Mob mob) {
        mob.setPersistent(false);
        mob.setRemoveWhenFarAway(false);
        Entities.setTransient(mob);
        if (mob instanceof Slime slime) {
            slime.setSize(3);
        } else if (mob instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
            zombie.setCanBreakDoors(false);
            final double babyChance;
            if (zombie instanceof Drowned) {
                babyChance = 0.1;
            } else if (zombie instanceof Husk) {
                babyChance = 0.1;
            } else if (zombie instanceof PigZombie pigZombie) {
                pigZombie.setAngry(true);
                babyChance = 0.1;
            } else if (zombie instanceof ZombieVillager) {
                babyChance = 0.1;
            } else { // regular zombie
                equipZombieOrSkeleton(zombie);
                zombie.getEquipment().setHelmet(Mytems.KOBOLD_HEAD.createItemStack());
                babyChance = 0.2;
            }
            if (game.getRandom().nextDouble() < babyChance) {
                zombie.setBaby();
            } else {
                zombie.setAdult();
            }
        } else if (mob instanceof AbstractSkeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
            if (skeleton instanceof Skeleton) {
                skeleton.getEquipment().setItemInMainHand(mobItem(Material.BOW));
                skeleton.getEquipment().setHelmet(SKELETON_SKULL.create());
                equipZombieOrSkeleton(skeleton);
            } else if (skeleton instanceof Stray) {
                skeleton.getEquipment().setItemInMainHand(mobItem(Material.BOW));
                equipZombieOrSkeleton(skeleton);
            } else if (skeleton instanceof WitherSkeleton) {
                if (game.getRandom().nextInt(2) == 0) {
                    skeleton.getEquipment().setItemInMainHand(mobItem(Material.BOW));
                } else {
                    skeleton.getEquipment().setItemInMainHand(mobItem(Material.IRON_SWORD));
                }
                skeleton.getEquipment().setArmorContents(new ItemStack[] {
                        mobItem(Material.NETHERITE_BOOTS),
                        mobItem(Material.NETHERITE_LEGGINGS),
                        mobItem(Material.NETHERITE_CHESTPLATE),
                        mobItem(Material.NETHERITE_HELMET),
                    });
            }
        } else if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
            hoglin.setIsAbleToBeHunted(false);
        } else if (mob instanceof Pillager pillager) {
            pillager.getEquipment().setItemInMainHand(mobItem(Material.CROSSBOW));
        } else if (mob instanceof Vindicator vindicator) {
            vindicator.getEquipment().setItemInMainHand(mobItem(Material.IRON_AXE));
        } else if (mob instanceof PiglinAbstract piglin) {
            if (game.getRandom().nextBoolean()) {
                piglin.getEquipment().setItemInMainHand(mobItem(Material.GOLDEN_AXE));
            } else {
                piglin.getEquipment().setItemInMainHand(mobItem(Material.GOLDEN_SWORD));
            }
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
        } else if (mob instanceof Evoker evoker) {
            evoker.getEquipment().setItemInMainHand(mobItem(Material.TOTEM_OF_UNDYING));
        } else if (mob instanceof Creeper creeper) {
            creeper.setMaxFuseTicks(Math.max(1, creeper.getMaxFuseTicks() - getEffectiveWave() / 2));
        }
        adjustAttributes(mob);
    }

    private static ItemStack mobItem(Material mat) {
        return new ItemBuilder(mat)
            .removeArmor()
            .removeDamage()
            .create();
    }

    protected void equipZombieOrSkeleton(Mob mob) {
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
        final int materialIndex = getEffectiveWave() / 20;
        mats.add(helmets.get(Math.min(materialIndex, helmets.size() - 1)));
        mats.add(chestplates.get(Math.min(materialIndex, chestplates.size() - 1)));
        mats.add(leggings.get(Math.min(materialIndex, leggings.size() - 1)));
        mats.add(boots.get(Math.min(materialIndex, boots.size() - 1)));
        mats.add(game.getRandom().nextBoolean()
                 ? swords.get(Math.min(materialIndex, swords.size() - 1))
                 : axes.get(Math.min(materialIndex, axes.size() - 1)));
        mats.add(materialIndex < 3 ? null : Material.SHIELD);
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
            ItemStack itemStack = mobItem(mat);
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

    /**
     * Modify the base value of a mob attribute.
     * @return success status
     */
    private boolean adjustAttribute(Mob mob, Attribute attribute, Function<Double, Double> func) {
        double base = 0.0;
        double value = 0.0;
        try {
            final AttributeInstance inst = mob.getAttribute(attribute);
            if (inst == null) return false;
            base = inst.getBaseValue();
            value = func.apply(base);
            inst.setBaseValue(value);
            if (attribute == Attribute.GENERIC_MAX_HEALTH) {
                mob.setHealth(value);
            }
            return true;
        } catch (Exception e) {
            game.getPlugin().getLogger().log(Level.WARNING, mob.getType() + " " + attribute + " " + base + " => " + value, e);
            return false;
        }
    }

    protected void adjustAttributes(Mob mob) {
        final double wave = (double) getEffectiveWave();
        adjustAttribute(mob, Attribute.GENERIC_ARMOR, base -> base + 0.1 * wave);
        adjustAttribute(mob, Attribute.GENERIC_ARMOR_TOUGHNESS, base -> base + 0.1 * wave);
        if (mob.getType() != EntityType.ENDERMAN) {
            adjustAttribute(mob, Attribute.GENERIC_ATTACK_DAMAGE, base -> base + 0.05 * wave);
        }
        adjustAttribute(mob, Attribute.GENERIC_MAX_HEALTH, base -> base + 0.2 * wave);
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

    /**
     * Scale arrow damage in a similar fashion to attack damage, see above.
     */
    @Override
    public void onProjectileLaunch(Projectile proj) {
        if (!(proj instanceof AbstractArrow arrow)) return;
        if (!(proj.getShooter() instanceof LivingEntity living)) return;
        Enemy enemy = Enemy.of(living);
        if (enemy == null || !game.getEnemies().contains(enemy)) return;
        double base = arrow.getDamage();
        double wave = (double) game.getTag().getCurrentWaveIndex();
        arrow.setDamage(base + 0.05 * wave);
    }

    /**
     * Fireballs do not set a damage value, so we manipulate the base
     * damage in the DamageCalculation.
     */
    @Override
    public void onDamageCalculation(DamageCalculationEvent event) {
        if (!event.targetIsPlayer() || !(event.getCalculation().getProjectile() instanceof Fireball fireball)) return;
        Enemy enemy = Enemy.of(event.getAttacker());
        if (enemy == null || !game.getEnemies().contains(enemy)) return;
        double base = event.getCalculation().getBaseDamage();
        double wave = (double) game.getTag().getCurrentWaveIndex();
        event.getCalculation().setBaseDamage(base + 0.05 * wave);
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
        lines.add(textOfChildren(text(Unicode.tiny("mobs "), GRAY),
                                 text(tag.getStillAlive(), GREEN)));
        lines.add(textOfChildren(text(Unicode.tiny("time "), GRAY),
                                 Time.format(runningTime)));
    }

    protected int getEffectiveWave() {
        return (game.getTag().getCurrentWaveIndex() / 10) * 10;
    }
}
