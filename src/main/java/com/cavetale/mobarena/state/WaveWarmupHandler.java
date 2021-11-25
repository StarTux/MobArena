package com.cavetale.mobarena.state;

import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.GameStateTag;
import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

final class WaveWarmupHandler extends GameStateHandler<GameStateTag> {
    protected static final Duration DURATION = Duration.ofSeconds(5);
    protected long secondsLeft = -1;

    protected WaveWarmupHandler(final Game game) {
        super(game, GameState.WAVE_WARMUP, GameStateTag.class, GameStateTag::new);
    }

    @Override
    public void onEnter() {
        game.makeNextWave();
        for (Player player : game.getPresentPlayers()) {
            player.showTitle(Title.title(Component.empty(), game.getCurrentWave().getDisplayName()));
        }
    }

    @Override
    public void onLoad() { }

    @Override
    public GameState tick() {
        Duration time = tag.getTime();
        if (time.toMillis() > DURATION.toMillis()) {
            for (Player player : game.getPresentPlayers()) {
                player.sendActionBar(Component.empty());
            }
            return GameState.WAVE;
        }
        Duration timeLeft = DURATION.minus(time);
        long seconds = (timeLeft.toMillis() - 1) / 1000L + 1L;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            for (Player player : game.getPresentPlayers()) {
                player.sendActionBar(Component.text(seconds + " Get ready!", NamedTextColor.GOLD));
            }
        }
        return null;
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        game.getCurrentWave().updateBossBar(bossBar);
    }
}
