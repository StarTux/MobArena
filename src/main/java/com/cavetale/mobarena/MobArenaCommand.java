package com.cavetale.mobarena;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.winthier.spawn.Spawn;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MobArenaCommand extends AbstractCommand<MobArenaPlugin> {
    protected MobArenaCommand(final MobArenaPlugin plugin) {
        super(plugin, "mobarena");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("join").denyTabCompletion()
            .hidden(true)
            .description("Join the event")
            .playerCaller(this::join);
        rootNode.addChild("stat").arguments("<stat> [domain]")
            .description("Change sidebar stat display")
            .completers(CommandArgCompleter.enumLowerList(Stat.class),
                        CommandArgCompleter.enumLowerList(StatDomain.class))
            .playerCaller(this::stat);
    }

    private void join(Player player) {
        if (plugin.tryToJoinEvent(player)) return;
        player.sendMessage(text("The event is not open", RED));
        Spawn.warp(player);
    }

    private boolean stat(Player player, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        final Stat stat = CommandArgCompleter.requireEnum(Stat.class, args[0]);
        final StatDomain domain = args.length >= 2
            ? CommandArgCompleter.requireEnum(StatDomain.class, args[1])
            : StatDomain.GAME;
        if (stat == Stat.ROUNDS && domain == StatDomain.WAVE) {
            throw new CommandWarn("Rounds are a global stat");
        }
        final Game game = plugin.getGameAt(player.getLocation());
        if (game == null) throw new CommandWarn("You are not in a game");
        final GamePlayer gamePlayer = game.getGamePlayer(player);
        gamePlayer.setSidebarStat(stat);
        gamePlayer.setSidebarStatDomain(domain);
        player.sendMessage(textOfChildren(text("Sidebar now displaying ", GRAY),
                                          text(stat.getDisplayName(), RED),
                                          text(" (" + domain.getDisplayText() + ")", GRAY)));
        return true;
    }
}
