package com.cavetale.mobarena;

import com.cavetale.enemy.Context;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.LivingEnemyWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public final class GameEnemyContext implements Context {
    protected final Game game;

    @Override
    public JavaPlugin getPlugin() {
        return game.getPlugin();
    }

    @Override
    public List<Player> getPlayers() {
        return game.getActivePlayers();
    }

    @Override
    public void registerNewEnemy(Enemy enemy) { }

    @Override
    public void registerTemporaryEntity(Entity entity) {
        entity.setPersistent(false);
        game.temporaryEntities.add(entity);
        if (entity instanceof LivingEntity living) {
            Enemy enemy = new LivingEnemyWrapper(this, living);
            game.enemies.add(enemy);
        }
    }

    @Override
    public boolean isTemporaryEntity(Entity entity) {
        return game.temporaryEntities.contains(entity);
    }

    @Override
    public int countTemporaryEntities(Class<? extends Entity> type) {
        int count = 0;
        for (Entity e : game.temporaryEntities) {
            if (type.isInstance(e)) count += 1;
        }
        return count;
    }

    @Override
    public void onDeath(Enemy enemy) {
        game.onDeath(enemy);
    }

    @Override
    public List<Enemy> getEnemies() {
        return game.getEnemies();
    }
}
