package com.cavetale.mobarena;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Stat {
    DAMAGE("Damage Dealt"),
    TAKEN("Damage Taken"),
    DEATHS("Deaths"),
    KILLS("Kills"),
    ROUNDS("Rounds Played");

    public final String displayName;
}
