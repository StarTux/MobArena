package com.cavetale.mobarena;

import com.cavetale.core.command.AbstractCommand;
import com.winthier.spawn.Spawn;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MobArenaCommand extends AbstractCommand<MobArenaPlugin> {
    protected MobArenaCommand(final MobArenaPlugin plugin) {
        super(plugin, "mobarena");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("join").denyTabCompletion()
            .description("Join the event")
            .playerCaller(this::join);
    }

    protected void join(Player player) {
        if (plugin.tryToJoinEvent(player)) return;
        player.sendMessage(text("The event is not open", RED));
        player.teleport(Spawn.get());
    }
}
