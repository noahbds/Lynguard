package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Invulnerable implements Component {
    public static final ComponentMapper<Invulnerable> MAPPER = ComponentMapper.getFor(Invulnerable.class);

    private float timeRemaining;

    public Invulnerable(float duration) {
        this.timeRemaining = duration;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(float timeRemaining) {
        this.timeRemaining = timeRemaining;
    }
    
    public void reduceTime(float amount) {
        this.timeRemaining -= amount;
    }
}
