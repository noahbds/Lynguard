package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Hazard implements Component {
    public enum HazardType {
        SPIKES,
        ENEMY,
        ENVIRONMENT
    }

    public static final ComponentMapper<Hazard> MAPPER = ComponentMapper.getFor(Hazard.class);

    private final HazardType type;
    private final float damage;
    private final boolean lethal;
    private boolean active;

    public Hazard(HazardType type, float damage, boolean lethal) {
        this.type = type;
        this.damage = damage;
        this.lethal = lethal;
        this.active = true;
    }

    public HazardType getType() {
        return type;
    }

    public float getDamage() {
        return damage;
    }

    public boolean isLethal() {
        return lethal;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
