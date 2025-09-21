package com.cavetale.mobarena.wave;

import com.cavetale.core.font.Unicode;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.EnemyType;
import com.cavetale.enemy.boss.LivingBoss;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.MobSpawnLocation;
import com.cavetale.mobarena.save.BossWaveTag;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class BossWave extends Wave<BossWaveTag> {
    public ArrayList<EnemyType> getBossTypes() {
        final int wave = game.getTag().getCurrentWaveIndex();
        ArrayList<EnemyType> result = new ArrayList<>();
        if (wave < 100) {
            result.add(EnemyType.QUEEN_BEE);
            result.add(EnemyType.HEINOUS_HEN);
            result.add(EnemyType.SNOBEAR);
            result.add(EnemyType.ICE_GOLEM);
        }
        result.add(EnemyType.INFERNAL_PHANTASM); // Blaze
        result.add(EnemyType.LAVA_LORD); // Magma Cube
        result.add(EnemyType.SPECTER); // Phantom
        result.add(EnemyType.GHAST_BOSS);
        if (wave >= 30) {
            result.add(EnemyType.DECAYED); // Wither Skeleton
            result.add(EnemyType.SKELLINGTON);
            result.add(EnemyType.ICEKELLY); // Stray
            result.add(EnemyType.FORGOTTEN); // Evoker
            result.add(EnemyType.FROSTWRECKER); // Drowned
            result.add(EnemyType.PIGLIN_BRUTE_BOSS);
        }
        if (wave >= 50) {
            result.add(EnemyType.VENGEFUL); // Wither
            result.add(EnemyType.WICKED_CRONE);
        }
        if (wave >= 100) {
            result.add(EnemyType.WARDEN_BOSS);
        }
        return result;
    }

    protected Component bossDisplayName = Component.empty();

    protected BossWave(final Game game) {
        super(game, WaveType.BOSS, BossWaveTag.class, BossWaveTag::new);
    }

    @Override
    public void create() {
        EnemyType enemyType;
        if (game.getNextBoss() != null) {
            enemyType = game.getNextBoss();
            game.setNextBoss(null);
        } else if (game.getTag().getCurrentWaveIndex() == 100) {
            enemyType = EnemyType.WARDEN_BOSS;
        } else {
            List<EnemyType> options = getBossTypes();
            options.removeAll(game.getTag().getDoneBosses());
            if (options.isEmpty()) {
                game.getTag().getDoneBosses().clear();
                options = getBossTypes();
            }
            enemyType = options.get(game.getRandom().nextInt(options.size()));
        }
        game.getTag().getDoneBosses().add(enemyType);
        tag.setEnemyType(enemyType);
        Enemy boss = tag.getEnemyType().create(game.getEnemyContext());
        tag.setBossEnemyId(boss.getEnemyId());
        game.getEnemies().add(boss);
        for (Player player : game.getPresentPlayers()) {
            player.showTitle(Title.title(Component.empty(), boss.getDisplayName()));
        }
    }

    @Override
    public void start() {
        spawnBoss();
    }

    @Override
    public void tick() {
        Enemy boss = getBoss();
        if (boss == null || boss.isDead()) {
            finished = true;
            game.getPlugin().getLogger().info("Boss Wave " + game.getTag().getCurrentWaveIndex() + " defeated by "
                                              + game.getPresentPlayers().stream().map(Player::getName)
                                              .collect(Collectors.joining(", ")));
        } else if (!boss.isValid()) { // isSpawned
            spawnBoss();
        } else {
            assert boss != null && boss.isValid();
            Location location = boss.getLocation();
            if (!game.getArena().isInArena(location) || game.getArena().isForbidden(location) || game.getArena().isBossEscape(location)) {
                if (boss instanceof LivingBoss livingBoss) {
                    final MobSpawnLocation mobSpawnLocation = game.getArena().getMobSpawnLocation(MobSpawnLocation.Type.BOSS,
                                                                                                  MobSpawnLocation.Environment.of(livingBoss.getMob().getType()));
                    mobSpawnLocation.respawn(livingBoss.getMob());
                } else {
                    final MobSpawnLocation mobSpawnLocation = game.getArena().getMobSpawnLocation(MobSpawnLocation.Type.BOSS, MobSpawnLocation.Environment.GROUND);
                    boss.teleport(mobSpawnLocation.random(boss.getWorld()));
                }
            }
        }
    }

    private void spawnBoss() {
        final Enemy boss = getBoss();
        if (boss == null) {
            game.getPlugin().getLogger().info("[" + game.getName() + "] " + game.getTag().getCurrentWaveIndex() + ": Boss is null: " + tag);
            return;
        }
        if (boss instanceof LivingBoss livingBoss) {
            final int wave = game.getTag().getCurrentWaveIndex();
            final int players = game.countActivePlayers();
            final int health = (players - 1) * 40 + (wave - 10) * 2 + 100;
            // Maximum health according to the Wiki:
            // https://minecraft.fandom.com/wiki/Attribute#Attributes
            final double max = 1024.0;
            livingBoss.setMaxHealth(Math.min(max, (double) health));
            final MobSpawnLocation mobSpawnLocation = game.getArena().getMobSpawnLocation(MobSpawnLocation.Type.BOSS,
                                                                                          MobSpawnLocation.Environment.of(livingBoss.getEntityType()));
            final Mob bossMob = mobSpawnLocation.spawn(game.getArena().getWorld(), location -> {
                    boss.spawn(location);
                    return boss.getMob() instanceof Mob mob
                        ? mob
                        : null;
                });
            if (bossMob != null) {
                boss.setSpawnLocation(bossMob.getLocation());
            }
        } else {
            final MobSpawnLocation mobSpawnLocation = game.getArena().getMobSpawnLocation(MobSpawnLocation.Type.BOSS, MobSpawnLocation.Environment.GROUND);
            final Location location = mobSpawnLocation.random(game.getArena().getWorld());
            boss.spawn(location);
            boss.setSpawnLocation(location);
        }
        boss.setDifficultyLevel(game.getTag().getCurrentWaveIndex() / 10);
    }

    @Override
    public void end() {
        for (Player player : game.getPresentPlayers()) {
            player.showTitle(Title.title(bossDisplayName, Component.text("Defeated!", GOLD)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.5f);
        }
        game.clearEnemies();
        game.clearTemporaryEntities();
    }

    @Override
    public void onDeath(Enemy enemy) {
        if (enemy == getBoss()) {
            finished = true;
        }
    }

    @Override
    public void onLoad() {
        Enemy boss = getBoss();
        if (boss == null) {
            create();
            return;
        }
        boss.setContext(game.getEnemyContext());
        game.getEnemies().add(boss);
    }

    /**
     * Get the boss entity previously which was created during
     * create().  It may or may not be spawned already.
     */
    public Enemy getBoss() {
        return Enemy.ofEnemyId(tag.getBossEnemyId());
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        Enemy boss = getBoss();
        if (boss == null) return;
        bossBar.color(BossBar.Color.RED);
        bossBar.overlay(BossBar.Overlay.PROGRESS);
        bossBar.name(boss.getDisplayName());
        double health = boss.getHealth() / boss.getMaxHealth();
        bossBar.progress(Math.max(0.0f, Math.min(1.0f, (float) health)));
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> lines) {
        Enemy boss = getBoss();
        if (boss == null) return;
        lines.add(textOfChildren(text(Unicode.tiny("boss "), GRAY), text((int) Math.round(boss.getHealth()), GREEN)));
    }
}
