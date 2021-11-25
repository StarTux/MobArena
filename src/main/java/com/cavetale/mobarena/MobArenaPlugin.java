package com.cavetale.mobarena;

import com.cavetale.area.struct.AreasFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobArenaPlugin extends JavaPlugin {
    protected final MobArenaCommand mobarenaCommand = new MobArenaCommand(this);
    protected final MobArenaAdminCommand mobarenaAdminCommand = new MobArenaAdminCommand(this);
    protected final EventListener eventListener = new EventListener(this);
    protected final Map<String, Arena> arenaMap = new HashMap<>();
    protected final List<Game> gameList = new ArrayList<>();
    protected File gamesFolder;
    protected Random random;

    @Override
    public void onEnable() {
        gamesFolder = new File(getDataFolder(), "games");
        gamesFolder.mkdirs();
        random = ThreadLocalRandom.current();
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

    protected void loadArenas() {
        World world = Bukkit.getWorld("halloween_arenas");
        for (Arena arena : loadArenas(world)) {
            arenaMap.put(arena.name, arena);
            arena.purge();
        }
    }

    protected void loadGames() {
        for (File file : gamesFolder.listFiles()) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            Game game = new Game(this, name);
            game.load();
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
            if (game.getArena().isInArena(location)) {
                return game;
            }
        }
        return null;
    }

    public void applyGame(Location location, Consumer<Game> consumer) {
        for (Game game : gameList) {
            if (game.getArena().isInArena(location)) {
                consumer.accept(game);
            }
        }
    }
}
