package com.cavetale.mobarena;

import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.util.Json;
import com.cavetale.mobarena.save.Config;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.cavetale.mytems.util.Items;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MobArenaPlugin extends JavaPlugin {
    protected static MobArenaPlugin instance;
    protected final MobArenaCommand mobarenaCommand = new MobArenaCommand(this);
    protected final MobArenaAdminCommand mobarenaAdminCommand = new MobArenaAdminCommand(this);
    protected final EventListener eventListener = new EventListener(this);
    protected Config config;
    protected final Map<String, Arena> arenaMap = new HashMap<>();
    protected final List<Game> gameList = new ArrayList<>();
    protected final Random random = ThreadLocalRandom.current();
    protected File gamesFolder;
    protected File configFile;
    public static final Component TITLE = join(noSeparators(), text("Mob", DARK_RED), text("ARENA", DARK_AQUA));
    protected List<String> worldNames = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        configFile = new File(getDataFolder(), "config.json");
        importConfig();
        gamesFolder = new File(getDataFolder(), "games");
        gamesFolder.mkdirs();
        mobarenaCommand.enable();
        mobarenaAdminCommand.enable();
        eventListener.enable();
        loadArenas();
        loadGames();
    }

    @Override
    public void onDisable() {
        for (Game game : gameList) {
            game.save();
            game.disable();
        }
        gameList.clear();
        arenaMap.clear();
    }

    protected void importConfig() {
        reloadConfig();
        if (!configFile.exists()) {
            Json.save(configFile, new Config(), true);
        }
        config = Json.load(configFile, Config.class, Config::new);
    }

    protected void exportConfig() {
        if (config == null) return;
        Json.save(configFile, config, true);
    }

    protected void loadArenas() {
        Set<String> loadWorldNames = new HashSet<>();
        loadWorldNames.add("halloween_arenas");
        loadWorldNames.addAll(getConfig().getStringList("Worlds"));
        getLogger().info("Worlds: " + loadWorldNames);
        for (String worldName : loadWorldNames) {
            loadArenas(worldName);
        }
    }

    protected void loadArenas(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().severe("Arena world not found: " + worldName);
            return;
        }
        worldNames.add(world.getName());
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
        world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, true);
        world.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);
        world.setGameRule(GameRule.DISABLE_RAIDS, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.DO_LIMITED_CRAFTING, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DROWNING_DAMAGE, true);
        world.setGameRule(GameRule.FALL_DAMAGE, true);
        world.setGameRule(GameRule.FIRE_DAMAGE, true);
        world.setGameRule(GameRule.FORGIVE_DEAD_PLAYERS, true);
        world.setGameRule(GameRule.FREEZE_DAMAGE, true);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, true);
        world.setGameRule(GameRule.MAX_COMMAND_CHAIN_LENGTH, 1);
        world.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 0);
        world.setGameRule(GameRule.MOB_GRIEFING, true); // Required for creeper explosions to register!
        world.setGameRule(GameRule.NATURAL_REGENERATION, true);
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, false);
        world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.UNIVERSAL_ANGER, true);
        world.setDifficulty(Difficulty.HARD);
        world.setTime(18000L);
        int count = 0;
        for (Arena arena : loadArenas(world)) {
            arenaMap.put(arena.name, arena);
            arena.purge();
            count += 1;
        }
        getLogger().info(worldName + ": " + count + " arenas loaded");
    }

    protected void loadGames() {
        for (File file : gamesFolder.listFiles()) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            Game game = new Game(this, name);
            try {
                game.load();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Loading " + file, e);
                file.delete();
                continue;
            }
            gameList.add(game);
            getLogger().info("Loaded game " + name);
        }
    }

    protected List<Arena> loadArenas(World world) {
        AreasFile areasFile = AreasFile.load(world, "MobArena");
        if (areasFile == null) return List.of();
        List<Arena> result = new ArrayList<>(areasFile.areas.size());
        for (var entry : areasFile.areas.entrySet()) {
            try {
                Arena arena = new Arena(world, entry.getKey(), entry.getValue());
                result.add(arena);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Loading Arena " + entry.getKey(), e);
            }
        }
        return result;
    }

    public Game startNewGame(Arena arena, String name) {
        Game game = new Game(this, name);
        game.start(arena);
        gameList.add(game);
        return game;
    }

    public Game startNewGame(String name) {
        Game game = new Game(this, name);
        game.start();
        gameList.add(game);
        return game;
    }

    public Game findGame(String name) {
        for (Game game : gameList) {
            if (name.equals(game.name)) return game;
        }
        return null;
    }

    public Game findGameInArena(Arena arena) {
        for (Game game : gameList) {
            if (arena.equals(game.arena)) return game;
        }
        return null;
    }

    public Game gameAt(Location location) {
        for (Game game : gameList) {
            if (game.getArena().isOnPlane(location) && game.getArena().isInWorld(location)) {
                return game;
            }
        }
        return null;
    }

    public boolean applyGame(Location location, Consumer<Game> consumer) {
        boolean result = false;
        for (Game game : gameList) {
            if (game.getArena().isOnPlane(location) && game.getArena().isInWorld(location)) {
                consumer.accept(game);
                result = true;
            }
        }
        return result;
    }

    public Arena randomUnusedArena() {
        List<String> options = new ArrayList<>(arenaMap.keySet());
        for (Game game : gameList) {
            options.remove(game.getArena().getName());
        }
        options.removeAll(config.getDisabledArenas());
        if (options.isEmpty()) return null;
        String arenaName = options.get(random.nextInt(options.size()));
        return arenaMap.get(arenaName);
    }

    public boolean isArenaInUse(Arena arena) {
        for (Game game : gameList) {
            if (game.getArena().getName().equals(arena.getName())) return true;
        }
        return false;
    }

    public Game findOrCreateGame() {
        Game game = null;
        for (Game it : gameList) {
            if (game == null || it.getName().equals("event")) {
                game = it;
            }
        }
        if (game != null) return game;
        Arena arena = randomUnusedArena();
        if (arena == null) return null;
        game = startNewGame(arena, UUID.randomUUID().toString());
        return game;
    }

    public Game getNearbyGame(Location location) {
        String worldName = location.getWorld().getName();
        for (Game game : gameList) {
            if (!worldName.equals(game.arena.getWorldName())) continue;
            if (!game.arena.isOnPlane(location)) continue;
            return game;
        }
        return null;
    }

    public boolean tryToJoinEvent(Player player) {
        Game eventGame = findGame("event");
        if (eventGame == null) return false;
        eventGame.addPlayer(player);
        eventGame.bring(player);
        player.sendMessage(text("Joined the event!", GREEN));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
        return true;
    }

    public void openJoinDialogue(Player player) {
        if (tryToJoinEvent(player)) {
            return;
        }
        if (config.isLocked()) {
            player.sendMessage(text("Please wait for Mob Arena to open its gates", GOLD));
            return;
        }
        int size = 3 * 9;
        Gui gui = new Gui(this)
            .title(GuiOverlay.BLANK.builder(size, BLUE)
                   .title(text("Join Mob Arena?", GOLD))
                   .build());
        gui.setItem(13, Items.text(Mytems.RUBY.createIcon(),
                                   List.of(text("Join Mob Arena for", GOLD),
                                           text("1 Ruby?", GOLD))),
                    evt -> {
                        for (ItemStack itemStack : player.getInventory()) {
                            if (!Mytems.RUBY.isItem(itemStack)) continue;
                            Game game = findOrCreateGame();
                            if (game == null) {
                                player.sendMessage(text("Something went wrong!", RED));
                                return;
                            }
                            itemStack.subtract(1);
                            player.closeInventory();
                            game.addPlayer(player);
                            game.bring(player);
                            return;
                        }
                        player.sendMessage(text("You don't have a Ruby!", RED));
                    });
        gui.open(player);
    }

    public boolean isArenaWorld(World world) {
        return worldNames.contains(world.getName());
    }
}
