# MobArena

MobArena plugin.

## Caveats

MobGriefing must be enabled for exploding creepers to count as dead!
Therefore we manually clear the blockList of exploding entities.

## Areas

Arenas use the Area plugin to define its regions.

Areas File: `MobArena`

Each arena has one area list, carrying the name of the arena.  The
first area of each list comprises the entirety of the legitimate area
of an arena.  All other regions have to be named like below.

### Required
- `spawn` Player spawns (air above ground)
- `mob`, `mobs` Where mobs spawn
- `bosschest` Where the boss chest goes (1x1x1)

### Optional
- `flyingmob`, `flyingmobs` Where flying mobs spawn. Defaults to `mob`
- `forbidden` Where players and mobs will be warped out of immediately
- `bossescape` Where bosses are warped out of immediately

## Config

The `config.yml` has the list of all worlds which should be scanned
for arenas.  The worlds have to be loaded.