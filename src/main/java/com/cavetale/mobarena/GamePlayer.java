package com.cavetale.mobarena;

import com.cavetale.mobarena.save.GamePlayerTag;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Data
public final class GamePlayer {
    protected final GamePlayerTag tag;

    public GamePlayer(final Player player) {
        this.tag = new GamePlayerTag();
        tag.setUuid(player.getUniqueId());
        tag.setName(player.getName());
    }

    public GamePlayer(final GamePlayerTag tag) {
        this.tag = tag;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(tag.getUuid());
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }
}
