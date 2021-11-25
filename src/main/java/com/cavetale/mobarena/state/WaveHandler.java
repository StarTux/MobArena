package com.cavetale.mobarena.state;

import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.GameStateTag;
import net.kyori.adventure.bossbar.BossBar;

final class WaveHandler extends GameStateHandler<GameStateTag> {
    protected WaveHandler(final Game game) {
        super(game, GameState.WAVE, GameStateTag.class, GameStateTag::new);
    }

    @Override
    public void onLoad() { }

    @Override
    public void onEnter() {
        game.getCurrentWave().start();
    }

    @Override
    public void onExit() {
        game.getCurrentWave().end();
    }

    @Override
    public GameState tick() {
        if (game.getCurrentWave().isFinished()) {
            return GameState.WAVE_COMPLETE;
        }
        game.getCurrentWave().tick();
        return null;
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        game.getCurrentWave().updateBossBar(bossBar);
    }
}
