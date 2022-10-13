package com.cavetale.mobarena.upgrade;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.mytems.util.Text.roman;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class EnchantmentUpgrade implements ItemUpgrade {
    private final Enchantment enchantment;
    private final int level;
    private final int requiredLevel;

    @Override
    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Override
    public void apply(ItemStack itemStack) {
        itemStack.editMeta(meta -> {
                meta.addEnchant(enchantment, level, true);
            });
    }

    @Override
    public Component getDescription() {
        return join(noSeparators(),
                    text("Add "),
                    Component.translatable(enchantment),
                    text(" " + roman(level)))
            .color(GREEN);
    }
}
