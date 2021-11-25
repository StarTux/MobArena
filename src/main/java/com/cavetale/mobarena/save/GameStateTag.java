package com.cavetale.mobarena.save;

import java.time.Duration;
import lombok.Data;

/**
 * Super class of all game state tags.
 */
@Data
public class GameStateTag {
    protected long startTime; // Set by Game

    public final Duration getTime() {
        return Duration.ofMillis(System.currentTimeMillis() - startTime);
    }
}
