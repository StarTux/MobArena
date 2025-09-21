package com.cavetale.mobarena;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static com.cavetale.core.font.Unicode.tiny;

@Getter
@RequiredArgsConstructor
public enum Stat {
    ROUNDS("Rounds Played", tiny("rnd")),
    KILLS("Kills", tiny("kls")),
    DEATHS("Deaths", tiny("dth")),
    DAMAGE("Damage Dealt", tiny("dmg")),
    TAKEN("Damage Taken", tiny("tkn")),
    ;

    private final String displayName;
    private final String tinyName;
}
