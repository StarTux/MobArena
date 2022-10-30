package com.cavetale.mobarena.upgrade;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class RepairUpgrade implements ItemUpgrade {
    private final int damage;

    @Override
    public int getRequiredLevel() {
        return 0;
    }

    @Override
    public void apply(ItemStack itemStack) {
        itemStack.editMeta(meta -> {
                if (meta instanceof Damageable damageable) {
                    damageable.setDamage(0);
                }
            });
    }

    @Override
    public Component getDescription() {
        return text("Repair " + damage + " damage", GREEN);
    }

    @Override
    public TextColor getHighlightColor() {
        return GREEN;
    }
}
