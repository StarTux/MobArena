package com.cavetale.mobarena.save;

import com.cavetale.enemy.EnemyType;
import com.cavetale.mobarena.Arena;
import com.cavetale.mobarena.state.GameState;
import com.cavetale.mobarena.wave.WaveType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Json structure.
 * Tags of the StateHandler and current Wave are stored serialized on
 * every save.  This is done because their deserialization will depend
 * on their respective type, expressed by their respective enums.
 */
@Data
public final class GameTag implements Serializable {
    // Arena
    protected Arena arena;
    protected List<String> usedArenaNames = new ArrayList<>();

    // GameState
    protected GameState state; // Enum
    protected String stateTag; // Serialized ? extends GameStateTag


    // Current Wave
    protected int currentWaveIndex;
    protected WaveType currentWaveType; // Enum
    protected String currentWaveTag; // Serialized ? extends WaveTag

    // Players
    protected List<GamePlayerTag> players = new ArrayList<>();

    // Waves
    protected List<EnemyType> doneBosses = new ArrayList<>();
}
