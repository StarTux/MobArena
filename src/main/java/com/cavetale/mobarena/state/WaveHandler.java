package com.cavetale.mobarena.state;

import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.Stat;
import com.cavetale.mobarena.save.GameStateTag;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

final class WaveHandler extends GameStateHandler<GameStateTag> {
    protected WaveHandler(final Game game) {
        super(game, GameState.WAVE, GameStateTag.class, GameStateTag::new);
    }

    @Override
    public void onLoad() { }

    @Override
    public void onEnter() {
        game.getCurrentWave().start();
        for (Player player : game.getActivePlayers()) {
            game.getGamePlayer(player).changeStat(Stat.ROUNDS, 1.0);
        }
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
    public void skip() {
        game.getCurrentWave().setFinished(true);
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        game.getCurrentWave().updateBossBar(bossBar);
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> list) {
        game.getCurrentWave().onPlayerSidebar(player, list);
    }
}
