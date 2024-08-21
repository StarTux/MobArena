package com.cavetale.mobarena;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import com.cavetale.enemy.EnemyType;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mobarena.state.RewardHandler;
import com.cavetale.mobarena.wave.Wave;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.BuildWorldPurpose;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
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
        rootNode.addChild("listgames").denyTabCompletion()
            .description("List Games")
            .senderCaller(this::listGames);
        rootNode.addChild("info").denyTabCompletion()
            .description("Game Info")
            .senderCaller(this::info);
        rootNode.addChild("debug").denyTabCompletion()
            .description("Debug Spam")
            .senderCaller(this::debug);
        rootNode.addChild("start").arguments("<name> [arena]")
            .completers(CommandArgCompleter.EMPTY,
                        CommandArgCompleter.supplyList(MobArenaAdminCommand::listArenaNames))
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
        rootNode.addChild("testreward").arguments("<level>")
            .description("Test the reward interface")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .playerCaller(this::testReward);
        rootNode.addChild("nextboss").arguments("<type>")
            .description("Set the next boss type")
            .completers(CommandArgCompleter.enumLowerList(EnemyType.class))
            .playerCaller(this::nextBoss);
        CommandNode eventNode = rootNode.addChild("event")
            .description("Event subcommands");
        eventNode.addChild("reward").denyTabCompletion()
            .description("Give event rewards")
            .senderCaller(this::eventReward);
        eventNode.addChild("lock").denyTabCompletion()
            .description("Lock regular arenas")
            .senderCaller(this::eventLock);
        eventNode.addChild("unlock").denyTabCompletion()
            .description("Unlock regular arenas")
            .senderCaller(this::eventUnlock);
        eventNode.addChild("start").denyTabCompletion()
            .description("Start the event game")
            .playerCaller(this::eventStart);
    }

    private static List<String> listArenaNames() {
        final List<String> result = new ArrayList<>();
        for (BuildWorld buildWorld : BuildWorld.findPurposeWorlds(BuildWorldPurpose.MOB_ARENA, false)) {
            result.add(buildWorld.getPath());
        }
        return result;
    }

    private void reload(CommandSender sender) {
        plugin.importConfig();
        sender.sendMessage(text("config.json reloaded", YELLOW));
    }

    private void listGames(CommandSender sender) {
        if (plugin.getGameList().isEmpty()) {
            throw new CommandWarn("No games running");
        }
        for (Game game : plugin.getGameList()) {
            sender.sendMessage(textOfChildren(text("Game " + game.getName(), YELLOW),
                                              text(" arena:", GRAY), text(game.getArena().getBuildWorld().getPath(), WHITE),
                                              text(" players:", GRAY), text(game.countActivePlayers(), WHITE)));
        }
    }

    private void info(CommandSender sender) {
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
    }

    protected void debug(CommandSender sender) {
        sender.sendMessage("Config: " + Json.prettyPrint(plugin.getMobArenaConfig()));
        for (Game game : plugin.gameList) {
            game.prepareForSaving();
            String pretty = Json.prettyPrint(game.getTag());
            sender.sendMessage(text("Game " + game.getName() + ": " + pretty, YELLOW));
        }
    }

    protected boolean start(Player player, String[] args) {
        if (args.length < 1 | args.length > 2) return false;
        final String name = args[0];
        final String arenaName = args.length >= 2
            ? args[1]
            : null;
        if (plugin.findGame(name) != null) {
            throw new CommandWarn("Game " + name + " already playing!");
        }
        if (arenaName != null) {
            final BuildWorld buildWorld = BuildWorld.findWithPath(arenaName);
            if (buildWorld == null) {
                throw new CommandWarn("Arena not found: " + arenaName);
            }
            if (buildWorld.getRow().parsePurpose() != BuildWorldPurpose.MOB_ARENA) {
                throw new CommandWarn("Not a MobArena world: " + arenaName);
            }
            buildWorld.makeLocalCopyAsync(world -> {
                    plugin.prepareArenaWorld(world);
                    final Arena arena = new Arena(world, buildWorld);
                    final Game game = plugin.startNewGame(arena, name);
                    game.addPlayer(player);
                    game.bring(player);
                });
            return true;
        }
        final Game game = plugin.startNewGame(name);
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

    private boolean testReward(Player player, String[] args) {
        if (args.length != 1) return false;
        final int level = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        new RewardHandler(new Game(plugin, "null")).openRewardChest(player, level);
        return true;
    }

    private boolean nextBoss(Player player, String[] args) {
        if (args.length == 0) {
            final Game game = plugin.gameAt(player.getLocation());
            if (game == null) {
                throw new CommandWarn("There is not game here");
            }
            final EnemyType nextBoss = game.getNextBoss();
            if (nextBoss == null) {
                throw new CommandWarn("Next boss was not set");
            }
            game.setNextBoss(null);
            player.sendMessage(text("Cleared next boss, was " + nextBoss, YELLOW));
            return true;
        } else if (args.length == 1) {
            final Game game = plugin.gameAt(player.getLocation());
            if (game == null) throw new CommandWarn("There is not game here");
            final String bossName = args[0];
            final EnemyType nextBoss = CommandArgCompleter.requireEnum(EnemyType.class, bossName);
            game.setNextBoss(nextBoss);
            player.sendMessage(text("Next boss set to " + nextBoss, YELLOW));
            return true;
        } else {
            return false;
        }
    }

    private void eventReward(CommandSender sender) {
        Game eventGame = plugin.findGame("event");
        if (eventGame == null) throw new CommandWarn("No event game!");
        int count = Highscore.reward(eventGame.getStatMap(Stat.DAMAGE),
                                     "mob_arena",
                                     TrophyCategory.AXE,
                                     MobArenaPlugin.TITLE,
                                     hi -> "You dealt " + hi.getScore() + " damage!");
        sender.sendMessage(text("Rewarded " + count + " players with trophies", YELLOW));
    }

    private void eventLock(CommandSender sender) {
        if (plugin.getMobArenaConfig().isLocked()) throw new CommandWarn("Already locked");
        plugin.getMobArenaConfig().setLocked(true);
        plugin.exportConfig();
        sender.sendMessage(text("Config locked", AQUA));
    }

    private void eventUnlock(CommandSender sender) {
        if (!plugin.getMobArenaConfig().isLocked()) throw new CommandWarn("Not locked");
        plugin.getMobArenaConfig().setLocked(false);
        plugin.exportConfig();
        sender.sendMessage(text("Config unlocked", AQUA));
    }

    protected void eventStart(Player player) {
        final String name = "event";
        if (plugin.findGame(name) != null) {
            throw new CommandWarn("Event already playing!");
        }
        final Arena arena;
        final Game game = plugin.startNewGame(name);
        game.addPlayer(player);
        game.bring(player);
        player.sendMessage(text("Event game started", AQUA));
    }
}
