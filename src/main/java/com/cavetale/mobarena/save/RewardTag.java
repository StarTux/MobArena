package com.cavetale.mobarena.save;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class RewardTag extends GameStateTag {
    protected UUID armorStandUuid;
    protected Set<UUID> playersClosedChest = new HashSet<>();
    protected long seed = System.currentTimeMillis();
}
