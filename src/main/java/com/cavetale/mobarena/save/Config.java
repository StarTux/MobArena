package com.cavetale.mobarena.save;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;

/**
 * Serializable config.json file.
 */
@Data
public final class Config {
    protected boolean locked;
    protected boolean allowFlight;
    protected Set<String> disabledArenas = new HashSet<>();
}
