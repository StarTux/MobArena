package com.cavetale.mobarena;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public final class MobArenaCommand extends AbstractCommand<MobArenaPlugin> {
    protected MobArenaCommand(final MobArenaPlugin plugin) {
        super(plugin, "mobarena");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Info Command")
            .senderCaller(this::info);
    }

    protected boolean info(CommandSender sender, String[] args) {
        return false;
    }
}
