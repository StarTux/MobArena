package com.cavetale.mobarena;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import com.cavetale.mobarena.state.RewardHandler;
import com.cavetale.mobarena.wave.Wave;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MobArenaAdminCommand extends AbstractCommand<MobArenaPlugin> {
    protected MobArenaAdminCommand(final MobArenaPlugin plugin) {
        super(plugin, "mobarenaadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload config.json")
            .senderCaller(this::reload);
        rootNode.addChild("list").denyTabCompletion()
            .description("List Arenas")
            .senderCaller(this::list);
        rootNode.addChild("info").denyTabCompletion()
            .description("Game Info")
            .senderCaller(this::info);
        rootNode.addChild("debug").denyTabCompletion()
            .description("Debug Spam")
            .senderCaller(this::debug);
        rootNode.addChild("start").arguments("[arena] [name]")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.arenaMap.keySet())),
                        CommandArgCompleter.NULL)
            .description("Start a game")
            .playerCaller(this::start);
        rootNode.addChild("stop").arguments("[game]")
            .completers(CommandArgCompleter.supplyList(() -> plugin.gameList.stream()
                                                       .map(Game::getName)
                                                       .collect(Collectors.toList())))
            .description("Stop a game")
            .senderCaller(this::stop);
        rootNode.addChild("skip").arguments("[game]")
            .completers(CommandArgCompleter.supplyList(() -> plugin.gameList.stream()
                                                       .map(Game::getName)
                                                       .collect(Collectors.toList())))
            .description("Skip a wave")
            .senderCaller(this::skip);
        rootNode.addChild("wave").arguments("<index>")
            .completers(CommandArgCompleter.integer(i -> i >= 0))
            .description("Change wave index")
            .playerCaller(this::wave);
        rootNode.addChild("joindialogue").arguments("<player>")
            .completers(CommandArgCompleter.NULL)
            .description("Show the join dialogue to a player")
            .senderCaller(this::joinDialogue);
        rootNode.addChild("addall").playerCaller(this::addall);
        rootNode.addChild("reward").arguments("<level>")
            .description("Test the reward interface")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .playerCaller(this::reward);
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.importConfig();
        sender.sendMessage(text("config.json reloaded", YELLOW));
        return true;
    }

    protected boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        if (plugin.arenaMap.isEmpty()) throw new CommandWarn("No arenas loaded");
        for (Map.Entry<String, Arena> entry : plugin.arenaMap.entrySet()) {
            sender.sendMessage("Arena " + entry.getKey() + ": " + entry.getValue().getArenaArea());
        }
        return true;
    }

    protected boolean info(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        if (plugin.gameList.isEmpty()) throw new CommandWarn("No games running");
        for (Game game : plugin.gameList) {
            List<Player> active = game.getActivePlayers();
            Wave<?> wave = game.getCurrentWave();
            Component msg = join(separator(newline()), new Component[] {
                    text("Game " + game.getName()),
                    text(" Wave #" + game.getTag().getCurrentWaveIndex()
                         + (wave == null ? "" : " " + wave.getWaveType().name().toLowerCase())),
                    text(" Players (" + active.size() + ") " + active.stream()
                         .map(Player::getName)
                         .collect(Collectors.joining(" "))),
                }).color(YELLOW);
            sender.sendMessage(msg);
        }
        return true;
    }

    protected boolean debug(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("Config: " + Json.prettyPrint(plugin.config));
        for (Game game : plugin.gameList) {
            game.prepareForSaving();
            String pretty = Json.prettyPrint(game.getTag());
            sender.sendMessage(text("Game " + game.getName() + ": " + pretty, YELLOW));
        }
        return true;
    }

    protected boolean start(Player player, String[] args) {
        if (args.length > 2) return false;
        String name = "admin";
        if (args.length >= 2) {
            name = args[1];
        }
        if (plugin.findGame(name) != null) {
            throw new CommandWarn("Game " + name + " already playing!");
        }
        final Arena arena;
        if (args.length >= 1) {
            String arenaName = args[0];
            arena = plugin.arenaMap.get(arenaName);
            if (arena == null) {
                throw new CommandWarn("Arena not found: " + arenaName);
            }
            if (plugin.findGameInArena(arena) != null) {
                throw new CommandWarn("Arena already playing: " + arenaName);
            }
        } else {
            List<String> options = new ArrayList<>(plugin.arenaMap.keySet());
            for (Game game : plugin.gameList) {
                options.remove(game.getArena().getName());
            }
            if (options.isEmpty()) {
                throw new CommandWarn("No empty arena found!");
            }
            String arenaName = options.get(plugin.random.nextInt(options.size()));
            arena = plugin.arenaMap.get(arenaName);
        }
        Game game = plugin.startNewGame(arena, name);
        game.addPlayer(player);
        game.bring(player);
        player.sendMessage(text("Game started: " + game.getName(), YELLOW));
        return true;
    }

    protected boolean stop(CommandSender sender, String[] args) {
        Game game;
        if (args.length >= 1) {
            String gameName = args[0];
            game = plugin.findGame(gameName);
            if (game == null) throw new CommandWarn("Game not found: " + gameName);
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            game = plugin.gameAt(player.getLocation());
            if (game == null) throw new CommandWarn("There is no game here");
        } else {
            throw new CommandWarn("[maaadm:stop] Player expected");
        }
        game.stop();
        sender.sendMessage(text("Game stopped: " + game.getName(), YELLOW));
        return true;
    }

    protected boolean skip(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        final Game game;
        if (args.length >= 1) {
            String gameName = args[0];
            game = plugin.findGame(gameName);
            if (game == null) throw new CommandWarn("Game not found: " + gameName);
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            game = plugin.gameAt(player.getLocation());
            if (game == null) throw new CommandWarn("There is no game here");
        } else {
            throw new CommandWarn("[maaadm:skip] Player expected");
        }
        game.getStateHandler().skip();
        sender.sendMessage(text("Skipping: " + game.getName(), YELLOW));
        return true;
    }

    protected boolean wave(Player player, String[] args) {
        if (args.length != 1) return false;
        final Game game = plugin.gameAt(player.getLocation());
        if (game == null) throw new CommandWarn("There is no game here");
        int waveIndex;
        try {
            waveIndex = Integer.parseUnsignedInt(args[0]);
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Invalid wave: " + args[0]);
        }
        game.getTag().setCurrentWaveIndex(waveIndex);
        player.sendMessage(text("Wave index updated: " + waveIndex, YELLOW));
        return true;
    }

    protected boolean joinDialogue(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) throw new CommandWarn("Player not found: " + args[0]);
        plugin.openJoinDialogue(target);
        return true;
    }

    protected boolean addall(Player player, String[] args) {
        Game game = plugin.gameAt(player.getLocation());
        if (game == null) throw new CommandWarn("game is null");
        for (Player other : player.getWorld().getPlayers()) {
            game.addPlayer(other);
            game.bring(other);
        }
        return true;
    }

    private boolean reward(Player player, String[] args) {
        if (args.length != 1) return false;
        final int level = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        new RewardHandler(new Game(plugin, "null")).openRewardChest(player, level);
        return true;
    }
}
