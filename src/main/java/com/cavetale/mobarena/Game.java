package com.cavetale.mobarena;

import com.cavetale.core.util.Json;
import com.cavetale.enemy.Enemy;
import com.cavetale.mobarena.save.GameTag;
import com.cavetale.mobarena.state.GameState;
import com.cavetale.mobarena.state.GameStateHandler;
import com.cavetale.mobarena.wave.Wave;
import com.cavetale.mobarena.wave.WaveType;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitTask;

/**
 * Runtime instance of a game of MobArena.
 *
 * A game is either started from scratch: start().
 * In that case it requires an Arena.
 *
 * Or it can be loaded from disk: load().
 * In that case it will pick its own arena.
 */
@Data
public final class Game {
    protected final MobArenaPlugin plugin;
    protected final String name;
    protected Arena arena;
    protected GameTag tag;
    protected File gameTagFile;
    protected Wave<?> currentWave;
    protected GameStateHandler<?> stateHandler;
    protected BukkitTask task;
    protected final List<Entity> temporaryEntities = new ArrayList<>();
    protected final GameEnemyContext enemyContext = new GameEnemyContext(this);
    protected final List<Enemy> enemies = new ArrayList<>();
    protected final Random random;
    protected final Map<UUID, GamePlayer> playerMap = new HashMap<>();
    protected final BossBar bossBar;

    public Game(final MobArenaPlugin plugin, final String name) {
        this.plugin = plugin;
        this.random = plugin.random;
        this.name = name;
        this.gameTagFile = new File(plugin.gamesFolder, name + ".json");
        this.bossBar = BossBar.bossBar(Component.empty(),
                                       0.0f,
                                       BossBar.Color.WHITE,
                                       BossBar.Overlay.PROGRESS);
    }

    protected void changeState(GameState newState) {
        GameState oldState = tag.getState();
        plugin.getLogger().info(name + " State " + oldState + " -> " + newState);
        GameStateHandler oldHandler = stateHandler;
        tag.setState(newState);
        GameStateHandler newHandler = newState.handlerCtor.apply(this);
        stateHandler = newHandler;
        if (oldHandler != null) {
            oldHandler.onExit();
        }
        newHandler.getTag().setStartTime(System.currentTimeMillis());
        newHandler.onEnter();
    }

    public void addPlayer(Player player) {
        getGamePlayer(player).getTag().setPlaying(true);
    }

    public GamePlayer getGamePlayer(Player player) {
        return playerMap.computeIfAbsent(player.getUniqueId(), u -> {
                player.showBossBar(bossBar);
                return new GamePlayer(player);
            });
    }

    protected void tick() {
        // Create players
        for (Player player : getPresentPlayers()) {
            getGamePlayer(player);
        }
        // Remove obsolete players
        for (var iter = playerMap.entrySet().iterator(); iter.hasNext();) {
            Player player = iter.next().getValue().getPlayer();
            if (player == null) {
                iter.remove();
            } else if (!arena.isInArena(player.getLocation())) {
                player.hideBossBar(bossBar);
                iter.remove();
            }
        }
        if (getActivePlayers().isEmpty()) {
            plugin.getLogger().info("Stopping because empty: " + name);
            stop();
            return;
        }
        temporaryEntities.removeIf(Entity::isDead);
        GameState newState = stateHandler.tick();
        if (newState != null) {
            changeState(newState);
        }
        stateHandler.updateBossBar(bossBar);
    }

    /**
     * Called at some point when the game is loaded or started.
     */
    protected void enable() {
        if (task != null) {
            throw new IllegalStateException("Task already exists");
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /**
     * Called at some point when the game stopped or saved for later.
     */
    protected void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Enemy enemy : enemies) {
            enemy.resetContext();
        }
        for (GamePlayer gamePlayer : playerMap.values()) {
            Player player = gamePlayer.getPlayer();
            if (player != null) {
                player.hideBossBar(bossBar);
            }
        }
        playerMap.clear();
        clearTemporaryEntities();
    }

    /**
     * Stop the game. It will not persist past this point.
     */
    protected void stop() {
        disable();
        clearEnemies();
        plugin.gameList.remove(this);
        gameTagFile.delete();
    }

    public void clearEnemies() {
        for (Enemy enemy : enemies) {
            enemy.remove();
        }
        enemies.clear();
    }

    /**
     * Start from scratch. Arena required.
     */
    protected void start(@NonNull Arena inArena) {
        this.arena = inArena;
        this.tag = new GameTag();
        tag.setArenaName(inArena.getName());
        changeState(GameState.PREPARE);
        enable();
    }

    /**
     * Load from disk.
     * This is an alternative to start().
     */
    protected void load() {
        tag = Objects.requireNonNull(Json.load(gameTagFile, GameTag.class));
        this.arena = plugin.arenaMap.get(tag.getArenaName());
        if (arena == null) {
            throw new IllegalStateException("Arena not found: " + tag.getArenaName());
        }
        this.stateHandler = tag.getState().handlerCtor.apply(this);
        stateHandler.load(tag.getStateTag());
        if (tag.getCurrentWaveType() != null) {
            this.currentWave = tag.getCurrentWaveType().waveCtor.apply(this);
            currentWave.load(tag.getCurrentWaveTag());
        }
        for (var gamePlayerTag : tag.getPlayers()) {
            GamePlayer gamePlayer = new GamePlayer(gamePlayerTag);
            playerMap.put(gamePlayerTag.getUuid(), gamePlayer);
            Player player = gamePlayer.getPlayer();
            if (player != null) player.showBossBar(bossBar);
        }
        stateHandler.onLoad();
        if (currentWave != null) currentWave.onLoad();
        enable();
    }

    protected void save() {
        stateHandler.onSave();
        tag.setStateTag(Json.serialize(stateHandler.getTag()));
        if (currentWave != null) {
            currentWave.onSave();
            tag.setCurrentWaveTag(Json.serialize(currentWave.getTag()));
        } else {
            tag.setCurrentWaveTag(null);
        }
        tag.setPlayers(new ArrayList<>());
        for (GamePlayer gamePlayer : playerMap.values()) {
            tag.getPlayers().add(gamePlayer.tag);
        }
        Json.save(gameTagFile, tag, true);
    }

    public void makeNextWave() {
        int waveIndex = tag.getCurrentWaveIndex() + 1;
        tag.setCurrentWaveIndex(waveIndex);
        WaveType waveType = waveIndex % 10 == 0
            ? WaveType.BOSS
            : WaveType.KILL;
        currentWave = waveType.waveCtor.apply(this);
        tag.setCurrentWaveType(currentWave.getWaveType());
        currentWave.create();
    }

    public List<Player> getPresentPlayers() {
        return arena.getPresentPlayers();
    }

    public List<Player> getActivePlayers() {
        List<Player> result = getPresentPlayers();
        result.removeIf(p -> !getGamePlayer(p).getTag().isPlaying());
        return result;
    }

    protected void clearTemporaryEntities() {
        for (Entity entity : temporaryEntities) {
            entity.remove();
        }
        temporaryEntities.clear();
    }

    protected void onDeath(Enemy enemy) {
        if (currentWave != null) {
            currentWave.onDeath(enemy);
        }
        enemy.remove();
        enemies.remove(enemy);
    }

    public void bring(Player player) {
        player.teleport(arena.randomSpawnLocation(), TeleportCause.PLUGIN);
    }

    protected void onPlayerSidebar(Player player, List<Component> lines) {
        if (currentWave != null) {
            lines.add(Component.text("Wave ", NamedTextColor.GRAY)
                      .append(Component.text(tag.getCurrentWaveIndex(), NamedTextColor.GREEN)));
        }
        stateHandler.onPlayerSidebar(player, lines);
    }

    protected void onCreatureSpawn(CreatureSpawnEvent event) {
        switch (event.getSpawnReason()) {
        case REINFORCEMENTS:
            event.setCancelled(true);
            break;
        case SLIME_SPLIT:
        case MOUNT:
            temporaryEntities.add(event.getEntity());
            break;
        default: break;
        }
    }
}
