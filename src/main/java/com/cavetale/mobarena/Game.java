package com.cavetale.mobarena;

import com.cavetale.core.font.Unicode;
import com.cavetale.core.util.Json;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.EnemyType;
import com.cavetale.mobarena.save.GameTag;
import com.cavetale.mobarena.state.GameState;
import com.cavetale.mobarena.state.GameStateHandler;
import com.cavetale.mobarena.wave.Wave;
import com.cavetale.mobarena.wave.WaveType;
import com.cavetale.mytems.event.combat.DamageCalculationEvent;
import com.cavetale.server.ServerPlugin;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.BuildWorldPurpose;
import com.winthier.spawn.Spawn;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.mobarena.util.Items.sendBrokenElytra;
import static com.winthier.creative.file.Files.deleteWorld;
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
    private EnemyType nextBoss;

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

    public Arena getArena() {
        if (tag == null) return null;
        return tag.getArena();
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
            if (!getArena().isOnPlane(player.getLocation())) {
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
            if (player.isGliding() && !plugin.getMobArenaConfig().isAllowFlight()) {
                player.setGliding(false);
                sendBrokenElytra(player);
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
            if (!getArena().isInWorld(playerLocation)) {
                gamePlayer.getTag().setPlaying(false);
                if (gamePlayer.bossBar) {
                    gamePlayer.bossBar = false;
                    player.hideBossBar(bossBar);
                }
            } else if (!getArena().isOnPlane(playerLocation) || getArena().isForbidden(playerLocation)) {
                bring(player);
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
        for (GamePlayer gamePlayer : playerMap.values()) {
            if (!gamePlayer.getTag().isPlaying()) continue;
            final Player player = gamePlayer.getPlayer();
            if (player == null) {
                gamePlayer.getTag().setPlaying(false);
                continue;
            }
            bring(player);
        }
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
        if (stateHandler != null) {
            stateHandler.onExit();
        }
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
        if (tag != null && tag.getArena() != null && tag.getArena().getWorld() != null) {
            for (Player player : tag.getArena().getWorld().getPlayers()) {
                Spawn.warp(player);
            }
            deleteWorld(tag.getArena().getWorld());
        }
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
    public void start(@NonNull Arena inArena) {
        this.tag = new GameTag();
        startCallback(inArena);
    }

    public void start() {
        this.tag = new GameTag();
        randomArena(this::startCallback);
    }

    private void startCallback(final Arena inArena) {
        tag.setArena(inArena);
        changeState(GameState.PREPARE);
        enable();
    }

    /**
     * Load from disk.
     * This is an alternative to start().
     */
    protected void load() {
        tag = Objects.requireNonNull(Json.load(gameTagFile, GameTag.class));
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

    /**
     * Called once by WaveWarmupHandler.
     */
    public void makeNextWave() {
        final int waveIndex = tag.getCurrentWaveIndex() + 1;
        tag.setCurrentWaveIndex(waveIndex);
        final WaveType waveType = waveIndex % 10 == 0
            ? WaveType.BOSS
            : WaveType.KILL;
        currentWave = waveType.waveCtor.apply(this);
        tag.setCurrentWaveType(currentWave.getWaveType());
        if (waveIndex > 1 && waveIndex % 10 == 1) {
            randomArena(newArena -> {
                    plugin.getLogger().info(name + " switching to arena " + newArena.getBuildWorldPath());
                    final List<Player> activePlayers = getActivePlayers();
                    final Arena oldArena = tag.getArena();
                    tag.setArena(newArena);
                    save();
                    for (Player player : activePlayers) {
                        bring(player);
                    }
                    if (oldArena != null) {
                        deleteWorld(oldArena.getWorld());
                    }
                    makeNextWaveCallback();
                });
        } else {
            makeNextWaveCallback();
        }
    }

    private void makeNextWaveCallback() {
        currentWave.create();
        for (GamePlayer gamePlayer : playerMap.values()) {
            gamePlayer.clearWaveStats();
        }
        if (plugin.gameList.indexOf(this) == 0 && !"admin".equals(name)) {
            List<Component> lines = List.of(text("/mobarena", GREEN),
                                            textOfChildren(text(tiny("players "), GRAY), text(countActivePlayers(), WHITE),
                                                           space(),
                                                           text(tiny("wave "), GRAY), text(tag.getCurrentWaveIndex(), WHITE)));
            ServerPlugin.getInstance().setServerSidebarLines(lines);
        }
    }

    private void randomArena(Consumer<Arena> callback) {
        final Map<String, BuildWorld> buildWorlds = new HashMap<>();
        for (BuildWorld buildWorld : BuildWorld.findPurposeWorlds(BuildWorldPurpose.MOB_ARENA, true)) {
            buildWorlds.put(buildWorld.getPath(), buildWorld);
        }
        final List<String> paths = new ArrayList<>(buildWorlds.keySet());
        // Prune used names
        paths.removeAll(tag.getUsedArenaNames());
        if (paths.isEmpty()) {
            tag.getUsedArenaNames().clear();
            paths.addAll(buildWorlds.keySet());
        }
        // Pick one
        final String thePath = paths.get(random.nextInt(paths.size()));
        tag.getUsedArenaNames().add(thePath);
        final BuildWorld buildWorld = buildWorlds.get(thePath);
        buildWorld.makeLocalCopyAsync(world -> {
                    plugin.prepareArenaWorld(world);
                    final Arena arena = new Arena(world, buildWorld);
                    callback.accept(arena);
            });
    }

    public List<Player> getPresentPlayers() {
        return getArena().getPresentPlayers();
    }

    public List<Player> getActivePlayers() {
        final List<Player> result = new ArrayList<>(playerMap.size());
        for (GamePlayer gamePlayer : playerMap.values()) {
            if (!gamePlayer.getTag().isPlaying()) continue;
            final Player player = gamePlayer.getPlayer();
            if (player == null) continue;
            switch (player.getGameMode()) {
            case ADVENTURE:
            case SURVIVAL:
                result.add(player);
            case CREATIVE:
            case SPECTATOR:
            default:
                break;
            }
        }
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
        if (tag == null || tag.getArena() == null) return;
        player.eject();
        player.leaveVehicle();
        player.teleport(getArena().randomSpawnLocation(), TeleportCause.PLUGIN);
        player.setGameMode(GameMode.ADVENTURE);
    }

    protected void onPlayerSidebar(Player player, List<Component> lines) {
        if (currentWave != null) {
            lines.add(textOfChildren(text(Unicode.tiny("wave "), GRAY),
                                     text(tag.getCurrentWaveIndex(), GREEN)));
        }
        stateHandler.onPlayerSidebar(player, lines);
        GamePlayer gamePlayer = getGamePlayer(player);
        if (gamePlayer != null && gamePlayer.getTag().isPlaying()) {
            lines.add(textOfChildren(text(Unicode.tiny("kills "), GRAY), text(gamePlayer.getIntWaveStat(Stat.KILLS), RED)));
            lines.add(textOfChildren(text(Unicode.tiny("dmg "), GRAY), text(gamePlayer.getIntWaveStat(Stat.DAMAGE), RED)));
        }
        lines.add(text(Unicode.tiny("total " + currentStat.displayName.toLowerCase()), RED));
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
        final double health = event.getTarget().getHealth();
        event.addPostDamageAction(() -> {
                if (event.targetIsPlayer()) {
                    final GamePlayer gamePlayer = getGamePlayer(event.getTargetPlayer());
                    final double value = Math.min(health, event.getCalculation().getTotalDamage());
                    if (value < 0.0) return;
                    gamePlayer.changeStat(Stat.TAKEN, value);
                } else if (event.attackerIsPlayer()) {
                    final GamePlayer gamePlayer = getGamePlayer(event.getAttackerPlayer());
                    final double value = Math.min(health, event.getCalculation().getTotalDamage());
                    if (value <= 0.0) return;
                    gamePlayer.changeStat(Stat.DAMAGE, value);
                }
            });
        if (currentWave != null) {
            currentWave.onDamageCalculation(event);
        }
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

    public void onPlayerDeath(PlayerDeathEvent event) {
        // Exp and level
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.setShouldDropExperience(false);
        // Inventory
        event.setKeepInventory(true);
        event.getDrops().clear();
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
        if (currentWave == null) return;
        currentWave.onProjectileLaunch(projectile);
    }
}
