package com.cavetale.mobarena.state;

import com.cavetale.core.font.GuiOverlay;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.RewardTag;
import com.cavetale.mobarena.util.Time;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import com.cavetale.mytems.MytemsTag;
import com.cavetale.mytems.util.Blocks;
import com.cavetale.mytems.util.Gui;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class RewardHandler extends GameStateHandler<RewardTag> {
    protected static final Duration DURATION = Duration.ofSeconds(120);
    protected boolean someoneClosed = false;
    protected boolean skipped;

    public RewardHandler(final Game game) {
        super(game, GameState.REWARD, RewardTag.class, RewardTag::new);
    }

    @Override
    public void onEnter() {
        Block block = game.getArena().bossChestBlock();
        List<BlockFace> blockFaces = List.of(BlockFace.NORTH,
                                             BlockFace.EAST,
                                             BlockFace.SOUTH,
                                             BlockFace.WEST);
        BlockFace face = blockFaces.get(game.getRandom().nextInt(blockFaces.size()));
        ArmorStand armorStand = Blocks.place(Mytems.BOSS_CHEST, block, face);
        block.setType(Material.BARRIER);
        getTag().setArmorStandUuid(armorStand.getUniqueId());
    };

    @Override
    public void onExit() {
        UUID uuid = getTag().getArmorStandUuid();
        if (uuid != null) {
            if (Bukkit.getEntity(uuid) instanceof ArmorStand as) {
                as.remove();
                getTag().setArmorStandUuid(null);
            }
        }
        Block block = game.getArena().bossChestBlock();
        block.setType(Material.AIR);
    };

    @Override
    public GameState tick() {
        Duration time = tag.getTime();
        boolean complete = time.toMillis() > DURATION.toMillis();
        if (someoneClosed) {
            someoneClosed = false;
            boolean allHave = true;
            for (Player player : game.getPresentPlayers()) {
                if (!getTag().getPlayersClosedChest().contains(player.getUniqueId())) {
                    allHave = false;
                    break;
                }
            }
            if (allHave) complete = true;
        }
        return (skipped || complete) ? GameState.WAVE_WARMUP : null;
    }

    @Override
    public void skip() {
        skipped = true;
    }

    @Override
    public void updateBossBar(BossBar bossBar) {
        bossBar.color(BossBar.Color.PINK);
        bossBar.overlay(BossBar.Overlay.PROGRESS);
        bossBar.progress(1.0f);
        bossBar.name(text("Open Your Reward!", GRAY, BOLD));
    }

    @Override
    public void onPlayerSidebar(Player player, List<Component> lines) {
        Duration timeLeft = DURATION.minus(tag.getTime());
        lines.add(join(noSeparators(),
                       text("Time ", GRAY),
                       Time.format(timeLeft)));
    }

    @Override
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!game.getGamePlayer(player).getTag().isPlaying()) return;
        Block block = event.getClickedBlock();
        if (!game.getArena().getBossChestVector().isSimilar(block)) return;
        if (getTag().getPlayersOpenedChest().contains(player.getUniqueId())) return;
        getTag().getPlayersOpenedChest().add(player.getUniqueId());
        openRewardChest(player);
        event.setUseItemInHand(Result.DENY);
        event.setUseInteractedBlock(Result.DENY);
    }

    private void openRewardChest(Player player) {
        int size = 3 * 9;
        Gui gui = new Gui(game.getPlugin())
            .title(GuiOverlay.BLANK.builder(size, color(0xFF69B4))
                   .title(text("Mob Arena Reward", BLACK))
                   .build());
        gui.setEditable(true);
        gui.setItem(13, getReward());
        gui.onClose(evt -> {
                for (ItemStack itemStack : gui.getInventory()) {
                    if (itemStack == null || itemStack.getType() == Material.AIR) continue;
                    for (ItemStack drop : player.getInventory().addItem(itemStack).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop);
                    }
                }
                getTag().getPlayersClosedChest().add(player.getUniqueId());
                someoneClosed = true;
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 1.0f, 1.0f);
            });
        gui.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 1.0f);
    }

    private ItemStack getReward() {
        List<ItemStack> pool = getRewardPool();
        if (pool.isEmpty()) return null;
        return pool.get(game.getRandom().nextInt(pool.size()));
    }

    private List<ItemStack> getRewardPool() {
        int lvl = game.getTag().getCurrentWaveIndex() / 10;
        if (lvl < 10) {
            return List.of(new ItemStack(Material.DIAMOND, 4),
                           new ItemStack(Material.GOLDEN_APPLE, 16),
                           new ItemStack(Material.NETHERITE_SCRAP, 8),
                           new ItemStack(Material.GUNPOWDER, 64),
                           new ItemStack(Material.BLAZE_ROD, 64),
                           new ItemStack(Material.TNT, 64),
                           new ItemStack(Material.BONE_BLOCK, 24),
                           new ItemStack(Material.ENCHANTED_GOLDEN_APPLE),
                           Mytems.KITTY_COIN.createItemStack());
        }
        if (lvl == 10) {
            return MytemsTag.of(MytemsCategory.SCARLET).getMytems().stream()
                .map(Mytems::createItemStack).collect(Collectors.toList());
        }
        return Stream.concat(MytemsTag.of(MytemsCategory.ARMOR_PART).getMytems().stream()
                             .map(Mytems::createItemStack),
                             Stream.of(Mytems.KITTY_COIN.createItemStack(),
                                       Mytems.RUBY.createItemStack(),
                                       Mytems.HEART.createItemStack(),
                                       Mytems.LIGHTNING.createItemStack(),
                                       Mytems.STAR.createItemStack(),
                                       Mytems.MOON.createItemStack(),
                                       Mytems.COPPER_COIN.createItemStack(),
                                       Mytems.SILVER_COIN.createItemStack(),
                                       Mytems.GOLDEN_COIN.createItemStack()))
            .collect(Collectors.toList());
    }
}
