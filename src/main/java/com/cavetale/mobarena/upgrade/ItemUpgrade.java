package com.cavetale.mobarena.upgrade;

import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

public interface ItemUpgrade {
    int getRequiredLevel();

    void apply(ItemStack itemStack);

    Component getDescription();
}
