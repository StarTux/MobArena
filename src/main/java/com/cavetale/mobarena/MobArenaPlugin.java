package com.cavetale.mobarena;

import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.cavetale.mytems.util.Items;
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
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MobArenaPlugin extends JavaPlugin {
    protected final MobArenaCommand mobarenaCommand = new MobArenaCommand(this);
    protected final MobArenaAdminCommand mobarenaAdminCommand = new MobArenaAdminCommand(this);
    protected final EventListener eventListener = new EventListener(this);
    protected final Map<String, Arena> arenaMap = new HashMap<>();
    protected final List<Game> gameList = new ArrayList<>();
    protected final Random random = ThreadLocalRandom.current();
    protected File gamesFolder;


    @Override
    public void onEnable() {
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

    protected void loadArenas() {
        World world = Bukkit.getWorld("halloween_arenas");
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
        world.setGameRule(GameRule.MOB_GRIEFING, false);
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

    public Arena randomUnusedArena() {
        List<String> options = new ArrayList<>(arenaMap.keySet());
        for (Game game : gameList) {
            options.remove(game.getArena().getName());
        }
        if (options.isEmpty()) return null;
        String arenaName = options.get(random.nextInt(options.size()));
        return arenaMap.get(arenaName);
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
        game = startNewGame(arena, arena.getName());
        return game;
    }

    public void openJoinDialogue(Player player) {
        Game eventGame = findGame("event");
        if (eventGame != null) {
            eventGame.addPlayer(player);
            eventGame.bring(player);
            player.sendMessage(text("Joined the event!", GREEN));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
            return;
        }
        int size = 3 * 9;
        Gui gui = new Gui(this)
            .title(GuiOverlay.BLANK.builder(size, GOLD)
                   .title(text("Join Mob Arena?", BLACK))
                   .build());
        gui.setItem(13, Items.text(Mytems.HALLOWEEN_TOKEN.createIcon(),
                                   List.of(text("Join Mob Arena for", GOLD),
                                           text("1 Halloween Token?", GOLD))),
                    evt -> {
                        for (ItemStack itemStack : player.getInventory()) {
                            if (!Mytems.HALLOWEEN_TOKEN.isItem(itemStack)) continue;
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
                        player.sendMessage(text("You don't have a Halloween Token!", RED));
                    });
        gui.open(player);
    }
}
