package com.cavetale.mobarena;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.entity.PlayerEntityAbilityQuery;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.player.PlayerTPAEvent;
import com.cavetale.core.event.skills.SkillsMobKillRewardEvent;
import com.cavetale.mytems.event.combat.DamageCalculationEvent;
import com.winthier.shutdown.event.ShutdownTriggerEvent;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import static com.cavetale.mobarena.util.Items.sendBrokenElytra;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final MobArenaPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPlayerHud(PlayerHudEvent event) {
        if (plugin.gameList.isEmpty()) return;
        Player player = event.getPlayer();
        List<Component> lines = new ArrayList<>();
        boolean inGame = plugin.applyGame(player.getLocation(), game -> {
                game.onPlayerSidebar(player, lines);
                event.bossbar(PlayerHudPriority.HIGHEST, game.getBossBar());
            });
        if (!inGame && !"admin".equals(plugin.gameList.get(0).getName())) {
            int waveIndex = plugin.gameList.get(0).getTag().getCurrentWaveIndex();
            if (waveIndex > 0) {
                lines.add(text("Mob Arena Wave " + waveIndex, GRAY));
            }
        }
        if (!lines.isEmpty()) {
            event.sidebar(PlayerHudPriority.HIGHEST, lines);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.isArenaWorld(event.getEntity().getWorld())) return;
        event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        plugin.applyGame(event.getEntity().getLocation(), game -> game.onCreatureSpawn(event));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || !plugin.isArenaWorld(event.getBlock().getWorld())) return;
        switch (event.getAction()) {
        case SPAWN_MOB: return;
        default: event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onEntityPlace(EntityPlaceEvent event) {
        if (!plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || !plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || !plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || !plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || !plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    private void onPlayerEntityAbility(PlayerEntityAbilityQuery event) {
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
    private void onPlayerInteract(PlayerInteractEvent event) {
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
    private void onPlayerItemDamage(PlayerItemDamageEvent event) {
        plugin.applyGame(event.getPlayer().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        switch (event.getCause()) {
        case ENDER_PEARL:
        case CONSUMABLE_EFFECT:
            plugin.applyGame(event.getPlayer().getLocation(), game -> event.setCancelled(true));
        default: break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerToggleGlide(EntityToggleGlideEvent event) {
        if (plugin.getMobArenaConfig().isAllowFlight()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.isGliding()) return;
        if (!plugin.isArenaWorld(player.getLocation().getWorld())) return;
        event.setCancelled(true);
        sendBrokenElytra(player);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.applyGame(event.getPlayer().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockIgnite(BlockIgniteEvent event) {
        if (!plugin.isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onHangingBreak(HangingBreakEvent event) {
        if (!plugin.isArenaWorld(event.getEntity().getWorld())) return;
        if (event instanceof HangingBreakByEntityEvent event2
            && event2.getRemover() instanceof Player player
            && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onShutdownTrigger(ShutdownTriggerEvent event) {
        if (!plugin.gameList.isEmpty()) {
            event.cancelBy(plugin);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerVoidDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        switch (event.getCause()) {
        case VOID: break;
        default: return;
        }
        final Game game = plugin.getGameIn(player.getLocation().getWorld());
        Bukkit.getScheduler().runTask(plugin, () -> {
                player.setFallDistance(0.0f);
                if (game == null) {
                    player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                } else {
                    game.bring(player);
                }
            });
    }

    @EventHandler(ignoreCancelled = true)
    private void onArmorStandDamage(EntityDamageEvent event) {
        final Game game = plugin.getGameIn(event.getEntity().getLocation().getWorld());
        if (game == null) return;
        if (game != null && event.getEntity() instanceof ArmorStand armorStand) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onDamageCalculation(DamageCalculationEvent event) {
        plugin.applyGame(event.getTarget().getLocation(), game -> game.onDamageCalculation(event));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityDeath(EntityDeathEvent event) {
        plugin.applyGame(event.getEntity().getLocation(), game -> game.onEntityDeath(event));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onPlayerDeath(PlayerDeathEvent event) {
        plugin.applyGame(event.getPlayer().getLocation(), game -> game.onPlayerDeath(event));
    }

    @EventHandler
    private void onPlayerTPA(PlayerTPAEvent event) {
        plugin.applyGame(event.getTarget().getLocation(), game -> event.setCancelled(true));
    }

    @EventHandler
    private void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity().getType() != EntityType.ENDERMAN) return;
        plugin.applyGame(event.getEntity().getLocation(), game -> {
                event.setTo(game.getArena().randomSpawnLocation());
            });
    }

    @EventHandler
    private void onProjectileLaunch(ProjectileLaunchEvent event) {
        plugin.applyGame(event.getEntity().getLocation(), game -> {
                game.onProjectileLaunch(event.getEntity());
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onSkillsMobKillReward(SkillsMobKillRewardEvent event) {
        plugin.applyGame(event.getPlayer().getLocation(), game -> {
                event.multiplyFactor(0.5 + (game.getTag().getCurrentWaveIndex() * 0.01 * 0.5));
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        final Game game = plugin.getGameIn(event.getPlayer().getLocation().getWorld());
        if (game == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerItemFrameChange(PlayerItemFrameChangeEvent event) {
        final Game game = plugin.getGameIn(event.getPlayer().getLocation().getWorld());
        if (game == null) return;
        event.setCancelled(true);
    }
}
