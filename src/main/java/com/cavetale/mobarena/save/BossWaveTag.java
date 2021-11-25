package com.cavetale.mobarena.save;

import com.cavetale.enemy.EnemyType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public final class BossWaveTag extends WaveTag {
    protected EnemyType enemyType;
    protected int bossEnemyId;
}
