package com.cavetale.mobarena.upgrade;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

@Getter
public final class UpgradableItem {
    private final ItemStack itemStack;
    private final int level;
    private final List<ItemUpgrade> upgrades = new ArrayList<>();

    public UpgradableItem(final ItemStack itemStack, final int level) {
        this.itemStack = itemStack;
        this.level = level;
        for (Enchantment enchantment : Enchantment.values()) {
            final boolean incompatible;
            if (enchantment.isCursed()) {
                continue;
            } else if (itemStack.getType() == Material.CROSSBOW && enchantment.equals(Enchantment.ARROW_INFINITE)) {
                incompatible = true;
            } else if (itemStack.getType() == Material.CROSSBOW && enchantment.equals(Enchantment.ARROW_FIRE)) {
                incompatible = true;
            } else if (!enchantment.canEnchantItem(itemStack)) {
                continue;
            } else {
                incompatible = false;
            }
            final int oldLevel = itemStack.getEnchantmentLevel(enchantment);
            final int maxLevel = enchantment == Enchantment.MULTISHOT
                ? 3
                : enchantment.getMaxLevel();
            if (oldLevel >= maxLevel) {
                continue;
            }
            int totalConflictingLevel = 0;
            boolean conflicts = false;
            INNER: for (Enchantment oldEnchant : Enchantment.values()) {
                if (enchantment.equals(oldEnchant)) continue INNER;
                int conflictingLevel = itemStack.getEnchantmentLevel(oldEnchant);
                if (conflictingLevel > 0 && enchantment.conflictsWith(oldEnchant)) {
                    totalConflictingLevel += conflictingLevel;
                    conflicts = true;
                }
            }
            int requiredLevel = oldLevel + 1;
            if (conflicts) {
                requiredLevel += 9 + totalConflictingLevel;
            } else if (incompatible) {
                requiredLevel += 10;
            }
            if (maxLevel > enchantment.getMaxLevel()) {
                requiredLevel += maxLevel - enchantment.getMaxLevel();
            }
            upgrades.add(new EnchantmentUpgrade(enchantment, oldLevel + 1, requiredLevel));
        }
    }

    public boolean isEmpty() {
        return upgrades.isEmpty();
    }
}
