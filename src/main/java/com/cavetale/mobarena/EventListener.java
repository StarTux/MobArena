package com.cavetale.mobarena;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.entity.PlayerEntityAbilityQuery;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
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
        plugin.applyGame(event.getEntity().getLocation(), game -> game.onCreatureSpawn(event));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onEntityChangeBlock(EntityChangeBlockEvent event) {
        plugin.applyGame(event.getBlock().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onPlayerBlockAbility(PlayerBlockAbilityQuery event) {
        plugin.applyGame(event.getBlock().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onEntityPlace(EntityPlaceEvent event) {
        plugin.applyGame(event.getEntity().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onBlockPlace(BlockPlaceEvent event) {
        plugin.applyGame(event.getBlock().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onBlockBreak(BlockBreakEvent event) {
        plugin.applyGame(event.getBlock().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        plugin.applyGame(event.getBlock().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    protected void onPlayerBucketFill(PlayerBucketFillEvent event) {
        plugin.applyGame(event.getBlock().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    protected void onPlayerEntityAbility(PlayerEntityAbilityQuery event) {
        plugin.applyGame(event.getEntity().getLocation(), game -> {
                switch (event.getAction()) {
                case DAMAGE:
                case IGNITE:
                case GIMMICK:
                case POTION:
                    return;
                default:
                    event.setCancelled(true);
                }
            });
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    protected void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
            plugin.applyGame(event.getClickedBlock().getLocation(), game -> {
                    game.onPlayerRightClickBlock(event);
                });
            break;
        default: return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerItemDamage(PlayerItemDamageEvent event) {
        plugin.applyGame(event.getPlayer().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerTeleport(PlayerTeleportEvent event) {
        switch (event.getCause()) {
        case ENDER_PEARL:
        case CHORUS_FRUIT:
            plugin.applyGame(event.getPlayer().getLocation(), game -> event.setCancelled(true));
        default: break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.isGliding()) {
            plugin.applyGame(player.getLocation(), game -> event.setCancelled(true));
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.applyGame(event.getPlayer().getLocation(), game -> event.setCancelled(true));
    }
}
