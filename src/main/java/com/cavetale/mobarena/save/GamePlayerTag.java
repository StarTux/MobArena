package com.cavetale.mobarena.save;

import java.util.UUID;
import lombok.Data;

@Data
public final class GamePlayerTag {
    protected UUID uuid;
    protected String name;
    protected boolean playing;
}
