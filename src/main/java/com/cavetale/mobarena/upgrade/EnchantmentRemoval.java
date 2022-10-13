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
public final class EnchantmentRemoval implements ItemUpgrade {
    private final Enchantment enchantment;
    private final int level;

    @Override
    public int getRequiredLevel() {
        return level + 1;
    }

    @Override
    public void apply(ItemStack itemStack) {
        itemStack.editMeta(meta -> {
                meta.removeEnchant(enchantment);
            });
    }

    @Override
    public Component getDescription() {
        return join(noSeparators(),
                    text("Remove "),
                    Component.translatable(enchantment),
                    text(" " + roman(level)))
            .color(RED);
    }
}
