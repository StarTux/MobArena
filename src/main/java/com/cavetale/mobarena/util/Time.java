package com.cavetale.mobarena.util;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Time {
    private Time() { }

    public static Component format(Duration duration) {
        long millis = duration.toMillis();
        long seconds = (millis - 1) / 1000L + 1L;
        long minutes = seconds / 60L;
        return Component.join(JoinConfiguration.noSeparators(),
                              Component.text(minutes),
                              Component.text("m", NamedTextColor.WHITE),
                              Component.space(),
                              Component.text(seconds % 60L, NamedTextColor.WHITE),
                              Component.text("s"));
    }
}
