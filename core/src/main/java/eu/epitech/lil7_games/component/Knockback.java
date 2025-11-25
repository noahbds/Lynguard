package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

public class Knockback implements Component {
    public static final ComponentMapper<Knockback> MAPPER = ComponentMapper.getFor(Knockback.class);

    private final Vector2 velocity;
    private float duration;

    public Knockback(Vector2 velocity, float duration) {
        this.velocity = velocity;
        this.duration = duration;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public float getDuration() {
        return duration;
    }

    public void reduceDuration(float amount) {
        this.duration -= amount;
    }
}
