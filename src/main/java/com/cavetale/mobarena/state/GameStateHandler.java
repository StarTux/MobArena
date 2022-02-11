package com.cavetale.mobarena.state;

import com.cavetale.core.util.Json;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.GameStateTag;
import java.util.List;
import java.util.function.Supplier;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Each subclass of this instance is responsible for running the
 * lifetime of a single GameState.  The enum in return holds a
 * reference to the constructor.
 *
 * GameStates are created and owned by Game.
 *
 * @param <T> The GameStateTag type
 */
public abstract class GameStateHandler<T extends GameStateTag> {
    protected final Game game;
    protected final Class<T> tagType;
    protected final Supplier<T> tagCtor;
    @Getter protected final GameState gameState;
    @Getter protected T tag;

    protected GameStateHandler(final Game game, final GameState gameState,
                               final Class<T> tagType, final Supplier<T> tagCtor) {
        this.game = game;
        this.gameState = gameState;
        this.tagType = tagType;
        this.tagCtor = tagCtor;
        this.tag = tagCtor.get();
    }

    public void onEnter() { };

    public abstract GameState tick();

    public void onExit() { };

    public void onLoad() { };

    public void onSave() { };

    public final void load(String serialized) {
        tag = Json.deserialize(serialized, tagType, tagCtor);
    }

    public void updateBossBar(BossBar bossBar) { }

    /**
     * Add lines to the sidebar.
     */
    public void onPlayerSidebar(Player player, List<Component> list) { }

    public void onPlayerRightClickBlock(PlayerInteractEvent event) { }
}
