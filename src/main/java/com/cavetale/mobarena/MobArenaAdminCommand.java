package com.cavetale.mobarena;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import com.cavetale.mobarena.state.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MobArenaAdminCommand extends AbstractCommand<MobArenaPlugin> {
    protected MobArenaAdminCommand(final MobArenaPlugin plugin) {
        super(plugin, "mobarenaadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Info Command")
            .senderCaller(this::info);
        rootNode.addChild("start").arguments("<arena>")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.arenaMap.keySet())))
            .description("Start a game")
            .playerCaller(this::start);
        rootNode.addChild("stop").arguments("<game>")
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
    }

    protected boolean info(CommandSender sender, String[] args) {
        for (Map.Entry<String, Arena> entry : plugin.arenaMap.entrySet()) {
            sender.sendMessage("Arena " + entry.getKey() + ": " + entry.getValue().getArenaArea());
        }
        for (Game game : plugin.gameList) {
            sender.sendMessage(Component.text("Game " + game.getName() + ": " + Json.serialize(game.getTag()),
                                              NamedTextColor.YELLOW));
        }
        return true;
    }

    protected boolean start(Player player, String[] args) {
        if (args.length > 1) return false;
        final String name = "admin";
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
        Game game = plugin.startNewGame(arena, "admin");
        game.addPlayer(player);
        game.bring(player);
        return true;
    }

    protected boolean stop(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String gameName = args[0];
        Game game = plugin.findGame(gameName);
        if (game == null) throw new CommandWarn("Game not found: " + gameName);
        game.stop();
        sender.sendMessage(Component.text("Game stopped: " + game.getName(), NamedTextColor.YELLOW));
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
        if (game.getTag().getState() != GameState.WAVE) {
            throw new CommandWarn("No wave is playing!");
        }
        game.getCurrentWave().setFinished(true);
        sender.sendMessage(Component.text("Skipping wave: " + game.getName(), NamedTextColor.YELLOW));
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
        player.sendMessage(Component.text("Wave index updated: " + waveIndex, NamedTextColor.YELLOW));
        return true;
    }
}
