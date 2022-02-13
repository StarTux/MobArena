package com.cavetale.mobarena.wave;

import com.cavetale.core.util.Json;
import com.cavetale.enemy.Enemy;
import com.cavetale.mobarena.Game;
import com.cavetale.mobarena.save.WaveTag;
import java.util.List;
import java.util.function.Supplier;
import lombok.Data;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Data
public abstract class Wave<T extends WaveTag> {
    protected final Game game;
    protected final WaveType waveType;
    protected T tag;
    protected final Class<T> tagType;
    protected final Supplier<T> tagCtor;
    protected boolean finished;

    protected Wave(final Game game, final WaveType waveType, final Class<T> tagType, final Supplier<T> tagCtor) {
        this.game = game;
        this.waveType = waveType;
        this.tagType = tagType;
        this.tagCtor = tagCtor;
        this.tag = tagCtor.get();
    }

    /**
     * Called immediately after creation, before the wave gets used in
     * any way.
     */
    public abstract void create();

    /**
     * Start this wave. Called before the first tick.
     */
    public abstract void start();

    /**
     * Called every tick.
     */
    public abstract void tick();

    /**
     * End this wave.
     */
    public abstract void end();

    /**
     * Load from the current tag.
     * Could be called during WaveWarmup, Wave, WaveComplete!
     */
    public void onLoad() { }

    /**
     * Store any runtim info in the tag.
     * Could be called during WaveWarmup, Wave, WaveComplete!
     */
    public void onSave() { }

    public final void load(String serialized) {
        tag = Json.deserialize(serialized, tagType, tagCtor);
    }

    public abstract void onDeath(Enemy enemy);

    public void updateBossBar(BossBar bossBar) { }

    public void onPlayerSidebar(Player player, List<Component> lines) { }
}
