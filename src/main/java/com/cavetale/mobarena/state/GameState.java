package com.cavetale.mobarena.state;

import com.cavetale.mobarena.Game;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameState {
    PREPARE(GamePrepareHandler::new),
    WAVE_WARMUP(WaveWarmupHandler::new),
    WAVE(WaveHandler::new),
    WAVE_COMPLETE(WaveCompleteHandler::new),
    REWARD(RewardHandler::new);

    public final Function<Game, GameStateHandler> handlerCtor;
}
