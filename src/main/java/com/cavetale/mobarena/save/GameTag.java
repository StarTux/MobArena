package com.cavetale.mobarena.save;

import com.cavetale.enemy.EnemyType;
import com.cavetale.mobarena.state.GameState;
import com.cavetale.mobarena.wave.WaveType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Json structure.
 */
@Data
public final class GameTag {
    protected GameState state;
    protected String stateTag;
    protected String arenaName;
    protected List<GamePlayerTag> players;
    protected int currentWaveIndex;
    protected WaveType currentWaveType;
    protected String currentWaveTag;
    protected List<EnemyType> doneBosses = new ArrayList<>();
}
