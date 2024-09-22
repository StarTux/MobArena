package com.cavetale.mobarena.upgrade;

import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import static io.papermc.paper.registry.RegistryAccess.registryAccess;

@Getter
public final class UpgradableItem {
    private final ItemStack itemStack;
    private final int level;
    private final List<ItemUpgrade> upgrades = new ArrayList<>();

    public UpgradableItem(final ItemStack itemStack, final int level) {
        this.itemStack = itemStack;
        this.level = level;
        for (Enchantment enchantment : registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
            if (enchantment.isCursed()) {
                continue;
            }
            final boolean semiIncompatible = isSemiIncompatible(enchantment);
            if (!semiIncompatible && !enchantment.canEnchantItem(itemStack)) {
                continue;
            }
            final int oldLevel = itemStack.getEnchantmentLevel(enchantment);
            final int maxLevel = enchantment.getMaxLevel();
            if (oldLevel >= maxLevel) {
                continue;
            }
            final int conflictLevel = hasConflicts(enchantment);
            int requiredLevel = oldLevel + 1;
            if (conflictLevel > 0) {
                requiredLevel += 9 + conflictLevel;
            } else if (semiIncompatible) {
                requiredLevel += 10;
            } else if (isRare(enchantment)) {
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

    /**
     * Semi incompatibility means that an item may or may not be
     * incompatible in vanilla.  Either way, we allow it, but at a
     * higher price.
     */
    public boolean isSemiIncompatible(Enchantment enchantment) {
        if (itemStack.getType() == Material.CROSSBOW) {
            return enchantment.equals(Enchantment.INFINITY) || enchantment.equals(Enchantment.FLAME);
        } else {
            return false;
        }
    }

    /**
     * Check if enchantments conflicts with others so we can up the
     * price.
     */
    public int hasConflicts(Enchantment enchantment) {
        int result = 0;
        for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
            final Enchantment oldEnchant = entry.getKey();
            if (enchantment.equals(oldEnchant)) {
                continue;
            }
            if (!enchantment.conflictsWith(oldEnchant)) {
                continue;
            }
            final int oldLevel = entry.getValue();
            result += oldLevel;
        }
        return result;
    }

    /**
     * Enchantments that are considered rare and hard to get in
     * vanilla will be more expensive here.
     */
    public static boolean isRare(Enchantment enchantment) {
        return enchantment.equals(Enchantment.WIND_BURST)
            || enchantment.equals(Enchantment.SWIFT_SNEAK)
            || enchantment.equals(Enchantment.SOUL_SPEED);
    }
}
