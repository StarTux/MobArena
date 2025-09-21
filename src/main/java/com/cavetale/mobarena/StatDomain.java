package com.cavetale.mobarena;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
@RequiredArgsConstructor
public enum StatDomain {
    GAME("Total", "whole game", NamedTextColor.RED),
    WAVE("Wave", "current wave", NamedTextColor.DARK_RED),
    ;

    private final String titlePrefix;
    private final String displayText;
    private final NamedTextColor color;
}
