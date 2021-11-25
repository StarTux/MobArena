package com.cavetale.mobarena.wave;

import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.EnemyType;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.BossWaveTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

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
        EnemyType.SADISTIC_VAMPIRE,
        EnemyType.WICKED_CRONE,
        EnemyType.INFERNAL_PHANTASM,
        EnemyType.GHAST_BOSS,
    };

    protected BossWave(final Game game) {
        super(game, WaveType.BOSS, BossWaveTag.class, BossWaveTag::new);
    }

    @Override
    public void create() {
        EnemyType enemyType = BOSS_TYPES[game.getRandom().nextInt(BOSS_TYPES.length)];
        tag.setEnemyType(enemyType);
        Enemy boss = tag.getEnemyType().create(game.getEnemyContext());
        tag.setBossEnemyId(boss.getEnemyId());
        displayName = boss.getDisplayName();
        game.getEnemies().add(boss);
    }

    @Override
    public void start() {
        Enemy boss = getBoss();
        if (boss != null) {
            boss.spawn(game.getArena().randomMobLocation());
        }
    }

    @Override
    public void tick() {
        Enemy boss = getBoss();
        if (boss == null || boss.isDead()) {
            finished = true;
        } else if (!boss.isValid()) { // isSpawned
            boss.spawn(game.getArena().randomMobLocation());
        }
    }

    @Override
    public void end() {
        for (Player player : game.getPresentPlayers()) {
            player.showTitle(Title.title(displayName,
                                         Component.text("Defeated!", NamedTextColor.GOLD)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.5f);
        }
    }

    @Override
    public void onDeath(Enemy enemy) {
        if (enemy == getBoss()) {
            finished = true;
        }
    }

    @Override
    public void onLoad() {
        Enemy boss = Enemy.ofEnemyId(tag.getBossEnemyId());
        if (boss == null) {
            create();
            return;
        }
        boss.setContext(game.getEnemyContext());
        game.getEnemies().add(boss);
        displayName = boss.getDisplayName();
    }

    public Enemy getBoss() {
        return Enemy.ofEnemyId(tag.getBossEnemyId());
    }
}
