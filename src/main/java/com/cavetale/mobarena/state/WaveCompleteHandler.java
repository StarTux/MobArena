package com.cavetale.mobarena.state;

import com.cavetale.core.event.minigame.MinigameFlag;
import com.cavetale.core.event.mobarena.MobArenaWaveCompleteEvent;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.GameStateTag;
import com.cavetale.mobarena.wave.WaveType;
import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class WaveCompleteHandler extends GameStateHandler<GameStateTag> {
    protected static final Duration DURATION = Duration.ofSeconds(2);
    protected boolean skipped;

    protected WaveCompleteHandler(final Game game) {
        super(game, GameState.WAVE_COMPLETE, GameStateTag.class, GameStateTag::new);
    }

    @Override
    public GameState tick() {
        Duration time = tag.getTime();
        if (skipped || time.toMillis() > DURATION.toMillis()) {
            if (game.getCurrentWave().getWaveType() == WaveType.BOSS) {
                return GameState.REWARD;
            } else {
                return GameState.WAVE_WARMUP;
            }
        }
        return null;
    }

    @Override
    public void onEnter() {
        MobArenaWaveCompleteEvent event = new MobArenaWaveCompleteEvent(game.getTag().getCurrentWaveIndex());
        if (game.isEvent()) event.addFlags(MinigameFlag.EVENT);
        event.addPlayerUuids(game.getPlayerMap().keySet());
        event.callEvent();
    }

    @Override
    public void skip() {
        skipped = true;
    }

    @Override
    public void onExit() {
        game.prunePlayers();
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        bossBar.color(BossBar.Color.WHITE);
        bossBar.overlay(BossBar.Overlay.PROGRESS);
        bossBar.progress(1.0f);
        bossBar.name(Component.text("Wave Complete", GRAY));
    }
}
