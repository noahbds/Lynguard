package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Damaged implements Component {
    public static final ComponentMapper<Damaged> MAPPER = ComponentMapper.getFor(Damaged.class);

    private float damage;
    private float sourceX;
    private float sourceY;

    public Damaged(float damage, float sourceX, float sourceY) {
        this.damage = damage;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
    }

    public void addDamage(float amount) {
        this.damage += amount;
    }

    public float getDamage() {
        return damage;
    }

    public float getSourceX() {
        return sourceX;
    }

    public float getSourceY() {
        return sourceY;
    }
}
