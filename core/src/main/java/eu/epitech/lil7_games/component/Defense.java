package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Defense component represents a timed block action.
 * When triggered, plays DEFEND animation once (NORMAL) and then ends automatically.
 * A cooldown prevents immediate retriggering.
 */
public class Defense implements Component {
    public static final ComponentMapper<Defense> MAPPER = ComponentMapper.getFor(Defense.class);

    private boolean defending;
    private float cooldownRemaining; // seconds
    private float defenseCooldown = 1.0f; // minimum time between defenses

    public void startDefense() {
        this.defending = true;
        this.cooldownRemaining = defenseCooldown; // start cooldown right away
    }

    public void stopDefense() {
        this.defending = false;
    }

    public void update(float delta) {
        if (cooldownRemaining > 0f) {
            cooldownRemaining = Math.max(0f, cooldownRemaining - delta);
        }
    }

    public boolean isDefending() { return defending; }
    public float getCooldownRemaining() { return cooldownRemaining; }
    public float getDefenseCooldown() { return defenseCooldown; }
    public void setDefenseCooldown(float defenseCooldown) { this.defenseCooldown = defenseCooldown; }
}
