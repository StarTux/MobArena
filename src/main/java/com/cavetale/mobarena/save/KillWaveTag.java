package com.cavetale.mobarena.save;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.EntityType;

@Data @EqualsAndHashCode(callSuper = true)
public final class KillWaveTag extends WaveTag {
    protected List<MobSpawn> mobSpawnList = new ArrayList<>();
    protected int stillAlive;
    protected int totalMobCount;

    @Data
    public static final class MobSpawn {
        protected EntityType entityType;
        protected int enemyId;
        protected boolean dead;
    }
}
