package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

public class Velocity implements Component {
    public static final ComponentMapper<Velocity> MAPPER = ComponentMapper.getFor(Velocity.class);

    private final Vector2 velocity;

    public Velocity(float x, float y) {
        this.velocity = new Vector2(x, y);
    }

    public Velocity() {
        this(0, 0);
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public void set(float x, float y) {
        velocity.set(x, y);
    }

    public void add(float x, float y) {
        velocity.add(x, y);
    }
}
