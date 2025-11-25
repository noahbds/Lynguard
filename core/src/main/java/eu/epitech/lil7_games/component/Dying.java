package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Marker component indicating the entity is playing its death animation and should be removed
 * after a short delay. Holds a countdown in seconds as a fallback if the animation is missing.
 */
public class Dying implements Component {
    public static final ComponentMapper<Dying> MAPPER = ComponentMapper.getFor(Dying.class);

    private float remainingSeconds;

    public Dying() { this(1.25f); }

    public Dying(float seconds) { this.remainingSeconds = Math.max(0f, seconds); }

    public void update(float delta) { this.remainingSeconds = Math.max(0f, this.remainingSeconds - delta); }

    public float getRemainingSeconds() { return remainingSeconds; }
}
