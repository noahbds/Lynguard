package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/** Component that disables gravity on an entity while present.
 *  It contains a remaining duration (seconds). When the remaining time
 *  reaches zero the component should be removed by a system (PlayerSystem).
 */
public class NoGravity implements Component {
    public static final ComponentMapper<NoGravity> MAPPER = ComponentMapper.getFor(NoGravity.class);

    private float remainingSeconds;

    /** Default duration is 3 seconds. */
    public NoGravity() { this(3.0f); }

    public NoGravity(float seconds) { this.remainingSeconds = Math.max(0f, seconds); }

    public float getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(float v) { this.remainingSeconds = Math.max(0f, v); }

    public void update(float delta) { this.remainingSeconds = Math.max(0f, this.remainingSeconds - delta); }
}
