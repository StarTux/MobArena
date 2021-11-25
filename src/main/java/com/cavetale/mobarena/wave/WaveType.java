package com.cavetale.mobarena.wave;

import com.cavetale.mobarena.Game;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WaveType {
    KILL(KillWave::new),
    BOSS(BossWave::new);

    public final Function<Game, ? extends Wave> waveCtor;
}
