package com.cavetale.mobarena.state;

import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.GameStateTag;
import com.cavetale.mobarena.util.Time;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

final class GamePrepareHandler extends GameStateHandler<GameStateTag> {
    protected static final Duration DURATION = Duration.ofSeconds(10);

    protected GamePrepareHandler(final Game game) {
        super(game, GameState.PREPARE, GameStateTag.class, GameStateTag::new);
    }

    @Override
    public GameState tick() {
        Duration time = tag.getTime();
        if (time.toMillis() > DURATION.toMillis()) {
            return GameState.WAVE_WARMUP;
        }
        return null;
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> lines) {
        Duration timeLeft = DURATION.minus(tag.getTime());
        lines.add(Time.format(timeLeft));
    }
}
