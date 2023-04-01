package com.cavetale.mobarena.state;

import com.cavetale.core.font.Unicode;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.GameStateTag;
import com.cavetale.mobarena.util.Time;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class GamePrepareHandler extends GameStateHandler<GameStateTag> {
    protected static final Duration DURATION = Duration.ofSeconds(60);
    protected static final Duration EVENT_DURATION = Duration.ofMinutes(5);
    protected boolean skipped;
    protected float progress;

    protected GamePrepareHandler(final Game game) {
        super(game, GameState.PREPARE, GameStateTag.class, GameStateTag::new);
    }

    @Override
    public GameState tick() {
        Duration time = tag.getTime();
        Duration duration = game.getName().equals("event") ? EVENT_DURATION : DURATION;
        progress = (float) time.toMillis() / (float) duration.toMillis();
        if (skipped || time.toMillis() > duration.toMillis()) {
            return GameState.WAVE_WARMUP;
        }
        return null;
    }

    @Override
    public void skip() {
        skipped = true;
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> lines) {
        Duration duration = game.getName().equals("event") ? EVENT_DURATION : DURATION;
        Duration timeLeft = duration.minus(tag.getTime());
        lines.add(join(noSeparators(),
                       text(Unicode.tiny("time "), GRAY),
                       Time.format(timeLeft)));
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        bossBar.color(BossBar.Color.WHITE);
        bossBar.overlay(BossBar.Overlay.PROGRESS);
        bossBar.progress(progress);
        bossBar.name(text("Preparing Arena", DARK_GRAY));
    }
}
