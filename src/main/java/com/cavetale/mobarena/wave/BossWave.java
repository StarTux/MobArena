package com.cavetale.mobarena.wave;

import com.cavetale.core.font.Unicode;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.EnemyType;
import com.cavetale.enemy.boss.LivingBoss;
import com.cavetale.mobarena.Game;
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
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class BossWave extends Wave<BossWaveTag> {
    protected  static final EnemyType[] BOSS_TYPES = {
        EnemyType.DECAYED,
        EnemyType.FORGOTTEN,
        EnemyType.VENGEFUL,
        EnemyType.SKELLINGTON,
        EnemyType.LAVA_LORD,
        EnemyType.FROSTWRECKER,
        EnemyType.ICE_GOLEM,
        EnemyType.ICEKELLY,
        EnemyType.SNOBEAR,
        EnemyType.QUEEN_BEE,
        EnemyType.HEINOUS_HEN,
        EnemyType.SPECTER,
        EnemyType.WICKED_CRONE,
        EnemyType.INFERNAL_PHANTASM,
        EnemyType.GHAST_BOSS,
        EnemyType.PIGLIN_BRUTE_BOSS,
    };
    protected Component bossDisplayName = Component.empty();

    protected BossWave(final Game game) {
        super(game, WaveType.BOSS, BossWaveTag.class, BossWaveTag::new);
    }

    @Override
    public void create() {
        EnemyType enemyType;
        if (game.getTag().getCurrentWaveIndex() == 100) {
            enemyType = EnemyType.WARDEN_BOSS;
        } else {
            List<EnemyType> options = new ArrayList<>(List.of(BOSS_TYPES));
            options.removeAll(game.getTag().getDoneBosses());
            if (options.isEmpty()) {
                enemyType = BOSS_TYPES[game.getRandom().nextInt(BOSS_TYPES.length)];
            } else {
                enemyType = options.get(game.getRandom().nextInt(options.size()));
                game.getTag().getDoneBosses().add(enemyType);
            }
        }
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
        }
    }

    private void spawnBoss() {
        Enemy boss = getBoss();
        if (boss == null) {
            game.getPlugin().getLogger().info("[" + game.getName() + "] " + game.getTag().getCurrentWaveIndex() + ": Boss is null: " + tag);
            return;
        }
        final Location location = switch (tag.getEnemyType()) {
        case QUEEN_BEE, SPECTER, GHAST_BOSS -> game.getArena().randomFlyingMobLocation();
        default -> game.getArena().randomMobLocation();
        };
        boss.setSpawnLocation(location);
        if (boss instanceof LivingBoss livingBoss) {
            double wave = (double) game.getTag().getCurrentWaveIndex();
            double players = (double) game.countActivePlayers();
            // +50 per boss level, +10 per player
            boss.setHealth(200 + 5.0 * wave + 10.0 * players);
        }
        boss.spawn(location);
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
