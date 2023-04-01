package com.cavetale.mobarena.save;

import com.cavetale.mobarena.Stat;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class GamePlayerTag {
    protected UUID uuid;
    protected String name;
    protected boolean playing;
    protected boolean didPlay;
    protected Map<Stat, Double> stats = new EnumMap<>(Stat.class);
    protected Map<Stat, Double> waveStats = new EnumMap<>(Stat.class);
}
