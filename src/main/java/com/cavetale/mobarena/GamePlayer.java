package com.cavetale.mobarena;

import com.cavetale.mobarena.save.GamePlayerTag;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Data
public final class GamePlayer {
    protected final GamePlayerTag tag;
    protected boolean bossBar;

    public GamePlayer(final Player player) {
        this.tag = new GamePlayerTag();
        tag.setUuid(player.getUniqueId());
        tag.setName(player.getName());
    }

    public GamePlayer(final GamePlayerTag tag) {
        this.tag = tag;
    }

    public UUID getUuid() {
        return tag.getUuid();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(tag.getUuid());
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public void changeStat(Stat stat, double value) {
        tag.getStats().compute(stat, (e, i) -> (i != null ? i + value : value));
    }

    public double getStat(Stat stat) {
        return tag.getStats().getOrDefault(stat, 0.0);
    }

    public int getIntStat(Stat stat) {
        Double result = tag.getStats().get(stat);
        if (result == null) return 0;
        return (int) Math.round(result);
    }
}
