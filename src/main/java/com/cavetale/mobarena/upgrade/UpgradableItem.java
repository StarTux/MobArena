package com.cavetale.mobarena.upgrade;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

@Getter
public final class UpgradableItem {
    private final ItemStack itemStack;
    private final int level;
    private final List<ItemUpgrade> upgrades = new ArrayList<>();

    public UpgradableItem(final ItemStack itemStack, final int level) {
        this.itemStack = itemStack;
        this.level = level;
        for (Enchantment enchantment : Enchantment.values()) {
            if (!enchantment.canEnchantItem(itemStack)) continue;
            final int oldLevel = itemStack.getEnchantmentLevel(enchantment);
            int conflictingLevel = 0;
            boolean conflicts = false;
            if (itemStack.getEnchantmentLevel(enchantment) == 0) {
                for (Enchantment oldEnchant : Enchantment.values()) {
                    if (enchantment == oldEnchant) continue;
                    conflictingLevel = itemStack.getEnchantmentLevel(oldEnchant);
                    if (conflictingLevel > 0 && enchantment.conflictsWith(oldEnchant)) {
                        conflicts = true;
                    }
                }
            }
            if (!enchantment.isCursed() && oldLevel < enchantment.getMaxLevel()) {
                final int requiredLevel = conflicts
                    ? oldLevel + conflictingLevel + 9
                    : oldLevel + 1;
                upgrades.add(new EnchantmentUpgrade(enchantment, oldLevel + 1, requiredLevel));
            }
        }
        if (itemStack.getItemMeta() instanceof Damageable damageable && damageable.getDamage() > 1) {
            upgrades.add(new RepairUpgrade(damageable.getDamage()));
        }
    }

    public boolean isEmpty() {
        return upgrades.isEmpty();
    }
}
