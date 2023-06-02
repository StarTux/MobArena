package com.cavetale.mobarena.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public final class Items {
    public static boolean sendBrokenElytra(Player player) {
        ItemStack item = player.getInventory().getChestplate();
        if (item == null || item.getType() != Material.ELYTRA) return false;
        item = item.clone();
        item.editMeta(meta -> {
                meta.setUnbreakable(false);
                ((Damageable) meta).setDamage(Material.ELYTRA.getMaxDurability());
            });
        player.sendEquipmentChange(player, EquipmentSlot.CHEST, item);
        return true;
    }

    private Items() { }
}
