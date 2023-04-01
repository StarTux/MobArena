package com.cavetale.mobarena;

import com.cavetale.core.font.Unicode;
import com.cavetale.core.util.Json;
import com.cavetale.enemy.Enemy;
import com.cavetale.mobarena.save.GameTag;
import com.cavetale.mobarena.state.GameState;
import com.cavetale.mobarena.state.GameStateHandler;
import com.cavetale.mobarena.wave.Wave;
import com.cavetale.mobarena.wave.WaveType;
import com.cavetale.mytems.event.combat.DamageCalculationEvent;
import com.cavetale.server.ServerPlugin;
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
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event.Result;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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
    protected Stat currentStat = Stat.DAMAGE;
    protected int currentStatTicks;

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
        getGamePlayer(player).getTag().setDidPlay(true);

    }

    public GamePlayer getGamePlayer(Player player) {
        return playerMap.computeIfAbsent(player.getUniqueId(), u -> {
                GamePlayer gamePlayer = new GamePlayer(player);
                tag.getPlayers().add(gamePlayer.getTag());
                return gamePlayer;
            });
    }

    public void prunePlayers() {
        for (GamePlayer gamePlayer : playerMap.values()) {
            Player player = gamePlayer.getPlayer();
            if (player == null) {
                gamePlayer.tag.setPlaying(false);
                continue;
            }
            if (!arena.isOnPlane(player.getLocation())) {
                gamePlayer.tag.setPlaying(false);
                continue;
            }
        }
    }

    protected void tick() {
        // Create players
        for (Player player : getPresentPlayers()) {
            GamePlayer gamePlayer = getGamePlayer(player);
            if (!gamePlayer.bossBar) {
                gamePlayer.bossBar = true;
                player.showBossBar(bossBar);
            }
        }
        // Remove obsolete players
        for (GamePlayer gamePlayer : playerMap.values()) {
            Player player = gamePlayer.getPlayer();
            if (player == null) {
                gamePlayer.getTag().setPlaying(false);
                continue;
            }
            Location playerLocation = player.getLocation();
            if (!arena.isOnPlane(playerLocation) || !arena.isInWorld(playerLocation)) {
                gamePlayer.getTag().setPlaying(false);
                if (gamePlayer.bossBar) {
                    gamePlayer.bossBar = false;
                    player.hideBossBar(bossBar);
                }
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
        currentStatTicks += 1;
        if (currentStatTicks > 1200) {
            currentStatTicks = 0;
            Stat[] stats = Stat.values();
            int statIndex = currentStat.ordinal() + 1;
            if (statIndex >= stats.length) statIndex = 0;
            currentStat = stats[statIndex];
        }
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
            if (gamePlayer.bossBar && player != null) {
                gamePlayer.bossBar = false;
                player.hideBossBar(bossBar);
            }
        }
        playerMap.clear();
        clearTemporaryEntities();
        if (plugin.gameList.indexOf(this) == 0 && !"admin".equals(name)) {
            ServerPlugin.getInstance().setServerSidebarLines(null);
        }
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
        }
        stateHandler.onLoad();
        if (currentWave != null) currentWave.onLoad();
        enable();
    }

    public void prepareForSaving() {
        stateHandler.onSave();
        tag.setStateTag(Json.serialize(stateHandler.getTag()));
        if (currentWave != null) {
            currentWave.onSave();
            tag.setCurrentWaveTag(Json.serialize(currentWave.getTag()));
        } else {
            tag.setCurrentWaveTag(null);
        }
    }

    protected void save() {
        prepareForSaving();
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
        if (waveIndex > 1 && waveIndex % 10 == 1) {
            Arena newArena = plugin.randomUnusedArena();
            if (newArena != null) {
                plugin.getLogger().info(name + " switching to arena " + newArena.getName());
                List<Player> activePlayers = getActivePlayers();
                this.arena = newArena;
                this.tag.setArenaName(newArena.getName());
                for (Player player : activePlayers) {
                    bring(player);
                }
            }
        }
        currentWave.create();
        if (plugin.gameList.indexOf(this) == 0 && !"admin".equals(name)) {
            List<Component> lines = List.of(text("/mobarena", GREEN),
                                            textOfChildren(text(tiny("players "), GRAY), text(countActivePlayers(), WHITE),
                                                           space(),
                                                           text(tiny("wave "), GRAY), text(waveIndex, WHITE)));
            ServerPlugin.getInstance().setServerSidebarLines(lines);
        }
    }

    public List<Player> getPresentPlayers() {
        return arena.getPresentPlayers();
    }

    public List<Player> getActivePlayers() {
        List<Player> result = getPresentPlayers();
        result.removeIf(p -> {
                switch (p.getGameMode()) {
                case ADVENTURE: case SURVIVAL: return false;
                case CREATIVE: case SPECTATOR: default: return true;
                }
            });
        return result;
    }

    public int countActivePlayers() {
        int count = 0;
        for (GamePlayer gamePlayer : playerMap.values()) {
            if (gamePlayer.getTag().isPlaying()) count += 1;
        }
        return count;
    }

    public void clearTemporaryEntities() {
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
        player.setGameMode(GameMode.ADVENTURE);
    }

    protected void onPlayerSidebar(Player player, List<Component> lines) {
        if (currentWave != null) {
            lines.add(textOfChildren(text(Unicode.tiny("wave "), GRAY),
                                     text(tag.getCurrentWaveIndex(), GREEN)));
        }
        stateHandler.onPlayerSidebar(player, lines);
        lines.add(text(Unicode.tiny(currentStat.displayName.toLowerCase()), RED));
        List<Player> players = getActivePlayers();
        players.sort((b, a) -> Double.compare(getGamePlayer(a).getStat(currentStat),
                                              getGamePlayer(b).getStat(currentStat)));
        for (Player it : players) {
            lines.add(textOfChildren(text("" + getGamePlayer(it).getIntStat(currentStat), RED),
                                     text(" "),
                                     it.displayName()));
        }
    }

    protected void onCreatureSpawn(CreatureSpawnEvent event) {
        switch (event.getSpawnReason()) {
        case CUSTOM:
        case COMMAND:
            return;
        case REINFORCEMENTS:
            event.setCancelled(true);
            break;
        case SLIME_SPLIT:
        case MOUNT:
        case SPELL:
        case DEFAULT: // Vex spawn with DEFAULT
            enemyContext.registerTemporaryEntity(event.getEntity());
            break;
        default: break;
        }
    }

    protected void onPlayerRightClickBlock(PlayerInteractEvent event) {
        event.setUseInteractedBlock(Result.DENY);
        stateHandler.onPlayerRightClickBlock(event);
    }

    public void onDamageCalculation(DamageCalculationEvent event) {
        event.addPostDamageAction(true, () -> {
                if (event.targetIsPlayer()) {
                    GamePlayer gamePlayer = getGamePlayer(event.getTargetPlayer());
                    double value = event.getCalculation().getTotalDamage();
                    if (value < 0.0) return;
                    gamePlayer.changeStat(Stat.TAKEN, value);
                } else if (event.attackerIsPlayer()) {
                    GamePlayer gamePlayer = getGamePlayer(event.getAttackerPlayer());
                    double value = event.getCalculation().getTotalDamage();
                    if (value <= 0.0) return;
                    gamePlayer.changeStat(Stat.DAMAGE, value);
                }
            });
        currentWave.onDamageCalculation(event);
    }

    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            GamePlayer gamePlayer = getGamePlayer(player);
            gamePlayer.changeStat(Stat.DEATHS, 1.0);
        } else {
            Player player = event.getEntity().getKiller();
            if (player == null) return;
            GamePlayer gamePlayer = getGamePlayer(player);
            gamePlayer.changeStat(Stat.KILLS, 1.0);
        }
    }

    public Map<UUID, Integer> getStatMap(Stat stat) {
        Map<UUID, Integer> result = new HashMap<>();
        for (GamePlayer gamePlayer : playerMap.values()) {
            result.put(gamePlayer.getUuid(), gamePlayer.getIntStat(stat));
        }
        return result;
    }

    public boolean isEvent() {
        return "event".equals(name);
    }

    protected void onProjectileLaunch(Projectile projectile) {
        currentWave.onProjectileLaunch(projectile);
    }
}
