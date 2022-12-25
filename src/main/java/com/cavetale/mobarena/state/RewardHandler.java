package com.cavetale.mobarena.state;

import com.cavetale.core.event.item.PlayerReceiveItemsEvent;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.RewardTag;
import com.cavetale.mobarena.upgrade.ItemUpgrade;
import com.cavetale.mobarena.upgrade.UpgradableItem;
import com.cavetale.mobarena.util.Time;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Blocks;
import com.cavetale.mytems.util.Entities;
import com.cavetale.mytems.util.Gui;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
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
        ArmorStand armorStand = Blocks.place(Mytems.BOSS_CHEST, block, face, true);
        armorStand.setPersistent(false);
        Entities.setTransient(armorStand);
        armorStand.setGlowing(true);
        getTag().setArmorStandUuid(armorStand.getUniqueId());
        block.setType(Material.BARRIER);
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
                       text(Unicode.tiny("time "), GRAY),
                       Time.format(timeLeft)));
    }

    @Override
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (!game.getArena().getBossChestVector().equals(Vec3i.of(block))) return;
        event.setUseItemInHand(Result.DENY);
        event.setUseInteractedBlock(Result.DENY);
        openRewardChest(player);
    }

    public void openRewardChest(Player player) {
        openRewardChest(player, game.getTag().getCurrentWaveIndex() / 10);
    }

    /**
     * Open rewards for player.
     * @param player the player
     * @param level the boss level, starting at 1
     */
    public void openRewardChest(Player player, final int level) {
        if (getTag().getPlayersClosedChest().contains(player.getUniqueId())) return;
        int size = 3 * 9;
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, color(0xFF69B4))
            .title(text("Choose a Reward", BLACK));
        Gui gui = new Gui(game.getPlugin());
        int nextSlot = 0;
        for (ItemStack item : player.getInventory()) {
            if (nextSlot >= size) break;
            if (item == null || item.getType().isAir()) continue;
            if (Mytems.forItem(item) != null) continue;
            UpgradableItem upgradableItem = new UpgradableItem(item, level);
            if (upgradableItem.isEmpty()) continue;
            ItemStack icon = item.clone();
            icon.editMeta(meta -> {
                    List<Component> lore = new ArrayList<>(meta.hasLore() ? meta.lore() : List.of());
                    lore.add(join(noSeparators(), Mytems.MOUSE_LEFT, text(" Upgrade this item", GREEN))
                             .decoration(ITALIC, false));
                    meta.lore(lore);
                });
            final int currentSlot = nextSlot++;
            gui.setItem(currentSlot, icon, click -> {
                    if (click.isLeftClick()) {
                        openUpgradableItem(player, upgradableItem, level);
                    }
                });
        }
        Random random = new Random(getTag().getSeed());
        List<ItemStack> pool = getRewardPool(player);
        Collections.shuffle(pool, random);
        final int maxItems = Math.min(3, level);
        for (int i = 0; i < maxItems; i += 1) {
            if (nextSlot >= size || i >= pool.size()) break;
            ItemStack reward = pool.get(i);
            ItemStack icon = reward.clone();
            icon.editMeta(meta -> {
                    List<Component> lore = new ArrayList<>(meta.hasLore() ? meta.lore() : List.of());
                    lore.add(join(noSeparators(), Mytems.MOUSE_LEFT, text(" Get this new item", GREEN))
                             .decoration(ITALIC, false));
                    meta.lore(lore);
                });
            final int currentSlot = nextSlot++;
            gui.setItem(currentSlot, icon, click -> {
                    if (!click.isLeftClick()) return;
                    openRewardItem(player, reward);
                });
            builder.highlightSlot(currentSlot, GOLD);
        }
        gui.title(builder.build());
        gui.open(player);
    }

    public void openUpgradableItem(Player player, UpgradableItem upgradableItem, final int level) {
        if (getTag().getPlayersClosedChest().contains(player.getUniqueId())) return;
        int size = 3 * 9;
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, color(0xFF69B4))
            .title(join(noSeparators(),
                        ItemKinds.icon(upgradableItem.getItemStack()),
                        text(" Choose an Upgrade", BLACK)));
        Gui gui = new Gui(game.getPlugin());
        int nextSlot = 0;
        for (ItemUpgrade itemUpgrade : upgradableItem.getUpgrades()) {
            if (nextSlot >= size) break;
            final boolean available = level >= itemUpgrade.getRequiredLevel();
            final ItemStack icon;
            if (!available) {
                icon = Mytems.COPPER_KEYHOLE.createIcon(List.of(text("Locked", DARK_RED)));
            } else {
                icon = upgradableItem.getItemStack().clone();
                itemUpgrade.apply(icon);
            }
            icon.editMeta(meta -> {
                    List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                    lore.add(join(noSeparators(),
                                  (available ? Mytems.MOUSE_LEFT : Mytems.COPPER_KEYHOLE),
                                  space(), itemUpgrade.getDescription())
                             .decoration(ITALIC, false));
                    if (!available) {
                        lore.add(text("Unlocks at wave " + (itemUpgrade.getRequiredLevel() * 10), RED));
                    }
                    meta.lore(lore);
                });
            final int currentSlot = nextSlot++;
            gui.setItem(currentSlot, icon, click -> {
                    if (!click.isLeftClick()) return;
                    if (!available) {
                        player.sendMessage(join(noSeparators(),
                                                Mytems.COPPER_KEYHOLE,
                                                text(" This upgrade unlocks at wave " + (itemUpgrade.getRequiredLevel() * 10), RED)));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                        return;
                    }
                    openItemUpgrade(player, upgradableItem, itemUpgrade, level);
                });
            if (!available) {
                builder.highlightSlot(currentSlot, DARK_GRAY);
            } else if (itemUpgrade.getHighlightColor() != null) {
                builder.highlightSlot(currentSlot, itemUpgrade.getHighlightColor());
            }
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                openRewardChest(player, level);
            });
        gui.title(builder.build());
        gui.open(player);
    }

    /**
     * Open the confirmation dialog for a specific item upgrade.
     */
    private void openItemUpgrade(Player player, UpgradableItem upgradableItem, ItemUpgrade itemUpgrade, int level) {
        final int size = 27;
        Gui gui = new Gui().size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, color(0xCC58A3))
            .title(join(noSeparators(),
                        ItemKinds.icon(upgradableItem.getItemStack()),
                        text(" ", BLACK),
                        itemUpgrade.getDescription()));
        ItemStack icon = upgradableItem.getItemStack().clone();
        itemUpgrade.apply(icon);
        icon.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(join(noSeparators(), Mytems.MOUSE_LEFT, text(" Confirm: ", GRAY), itemUpgrade.getDescription()));
                meta.lore(lore);
            });
        gui.setItem(11, icon, click -> {
                if (!click.isLeftClick()) return;
                if (level < itemUpgrade.getRequiredLevel()) return;
                if (getTag().getPlayersClosedChest().contains(player.getUniqueId())) return;
                getTag().getPlayersClosedChest().add(player.getUniqueId());
                someoneClosed = true;
                itemUpgrade.apply(upgradableItem.getItemStack());
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 1.0f);
                player.sendMessage(join(noSeparators(),
                                        text("Upgrade received: "),
                                        ItemKinds.icon(upgradableItem.getItemStack()),
                                        space(),
                                        itemUpgrade.getDescription()));
                player.closeInventory();
            });
        gui.setItem(15, Mytems.TURN_LEFT.createIcon(List.of(text("Go Back", GRAY))), click -> {
                if (!click.isLeftClick()) return;
                openUpgradableItem(player, upgradableItem, level);
            });
        gui.setItem(Gui.OUTSIDE, null, click -> openUpgradableItem(player, upgradableItem, level));
        gui.title(builder.build());
        gui.open(player);
    }

    /**
     * Open a GUI with a single reward item, presumably coming from the pool.
     */
    public void openRewardItem(Player player, ItemStack rewardItem) {
        if (getTag().getPlayersClosedChest().contains(player.getUniqueId())) return;
        int size = 3 * 9;
        Gui gui = new Gui(game.getPlugin())
            .title(GuiOverlay.BLANK.builder(size, color(0xFF69B4))
                   .title(text("Choose an upgrade", BLACK))
                   .build());
        gui.setEditable(true);
        gui.setItem(13, rewardItem);
        gui.onClose(evt -> {
                PlayerReceiveItemsEvent.receiveInventory(player, gui.getInventory());
                getTag().getPlayersClosedChest().add(player.getUniqueId());
                someoneClosed = true;
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 1.0f, 1.0f);
            });
        gui.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 1.0f);
        player.sendMessage(join(noSeparators(),
                                text("Item received: "),
                                ItemKinds.chatDescription(rewardItem)));
    }

    private List<ItemStack> getRewardPool(Player player) {
        Random random = new Random(getTag().getSeed());
        List<ItemStack> pool = new ArrayList<>();
        pool.addAll(List.of(new ItemStack(Material.DIAMOND, 1 + random.nextInt(64)),
                            new ItemStack(Material.EMERALD, 64),
                            new ItemStack(Material.IRON_INGOT, 64),
                            new ItemStack(Material.GOLD_INGOT, 64),
                            new ItemStack(Material.GOLDEN_APPLE, 1 + random.nextInt(32)),
                            new ItemStack(Material.NETHERITE_SCRAP, 1 + random.nextInt(10)),
                            new ItemStack(Material.GUNPOWDER, 64),
                            new ItemStack(Material.GLOWSTONE, 64),
                            new ItemStack(Material.TNT, 64),
                            new ItemStack(Material.BLAZE_ROD, 64),
                            new ItemStack(Material.BONE_BLOCK, 64),
                            new ItemStack(Material.ENCHANTED_GOLDEN_APPLE),
                            new ItemStack(Material.NETHER_STAR),
                            Mytems.KITTY_COIN.createItemStack(),
                            Mytems.RUBY.createItemStack(1 + random.nextInt(16)),
                            Mytems.SILVER_COIN.createItemStack(1 + random.nextInt(10)),
                            Mytems.GOLDEN_COIN.createItemStack(1 + random.nextInt(3)),
                            Mytems.DIAMOND_COIN.createItemStack(1 + random.nextInt(3))));
        List<Enchantment> enchantments = new ArrayList<>();
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.isCursed()) continue;
            enchantments.add(enchantment);
        }
        final Enchantment enchantment = enchantments.get(random.nextInt(enchantments.size()));
        final int level = enchantment.getMaxLevel() > 1
            ? 1 + random.nextInt(enchantment.getMaxLevel() - 1)
            : 1;
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        book.editMeta(m -> {
                if (m instanceof EnchantmentStorageMeta meta) {
                    meta.addStoredEnchant(enchantment, level, true);
                }
            });
        pool.add(book);
        return pool;
    }
}
