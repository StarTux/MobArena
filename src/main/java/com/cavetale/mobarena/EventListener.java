package com.cavetale.mobarena;

import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import com.winthier.spawn.Spawn;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final MobArenaPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Location at = event.getSpawnLocation();
        for (Arena arena : plugin.arenaMap.values()) {
            if (arena.isInArena(at)) {
                event.setSpawnLocation(Spawn.get());
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onPlayerSidebar(PlayerSidebarEvent event) {
        Player player = event.getPlayer();
        List<Component> lines = new ArrayList<>();
        plugin.applyGame(player.getLocation(), game -> {
                game.onPlayerSidebar(player, lines);
            });
        if (!lines.isEmpty()) {
            event.add(plugin, Priority.HIGHEST, lines);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onEntityExplode(EntityExplodeEvent event) {
        plugin.applyGame(event.getEntity().getLocation(), game -> event.blockList().clear());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onBlockExplode(BlockExplodeEvent event) {
        plugin.applyGame(event.getBlock().getLocation(), game -> event.blockList().clear());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    protected void onCreatureSpawn(CreatureSpawnEvent event) {
        plugin.applyGame(event.getEntity().getLocation(), game -> {
                game.onCreatureSpawn(event);
            });
    }
}
