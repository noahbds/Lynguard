package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/** Tracks a cooldown timer for the anti-gravity power on a per-entity basis. */
public class NoGravityCooldown implements Component {
    public static final ComponentMapper<NoGravityCooldown> MAPPER = ComponentMapper.getFor(NoGravityCooldown.class);

    private float remainingSeconds;

    public NoGravityCooldown() { this(1.5f); }
    public NoGravityCooldown(float seconds) { this.remainingSeconds = Math.max(0f, seconds); }

    public float getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(float v) { this.remainingSeconds = Math.max(0f, v); }
    public void update(float delta) { this.remainingSeconds = Math.max(0f, this.remainingSeconds - delta); }
}
