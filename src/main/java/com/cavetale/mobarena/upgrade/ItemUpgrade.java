package com.cavetale.mobarena.upgrade;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;

public interface ItemUpgrade {
    int getRequiredLevel();

    void apply(ItemStack itemStack);

    Component getDescription();

    default TextColor getHighlightColor() {
        return null;
    }
}
