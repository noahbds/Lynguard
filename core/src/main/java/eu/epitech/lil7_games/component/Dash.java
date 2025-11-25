package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

public class Dash implements Component {
    public static final ComponentMapper<Dash> MAPPER = ComponentMapper.getFor(Dash.class);

    private boolean charging;
    private boolean dashing;
    private float chargeTime;
    private int chargeLevel = 1;
    private float cooldownRemaining;
    private float dashTimeRemaining;
    private final Vector2 dashVelocity = new Vector2();
    private final Vector2 lastDirection = new Vector2(1f, 0f);
    private float holdTime;

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public boolean isDashing() {
        return dashing;
    }

    public void startDash(Vector2 velocity, float duration) {
        this.dashing = true;
        this.dashTimeRemaining = duration;
        this.dashVelocity.set(velocity);
        if (!velocity.isZero()) {
            this.lastDirection.set(velocity).nor();
        }
    }

    public void stopDash() {
        this.dashing = false;
        this.dashTimeRemaining = 0f;
        this.dashVelocity.setZero();
    }

    public float getChargeTime() {
        return chargeTime;
    }

    public void setChargeTime(float chargeTime) {
        this.chargeTime = chargeTime;
    }

    public int getChargeLevel() {
        return chargeLevel;
    }

    public void setChargeLevel(int chargeLevel) {
        this.chargeLevel = chargeLevel;
    }

    public float getHoldTime() {
        return holdTime;
    }

    public void setHoldTime(float holdTime) {
        this.holdTime = Math.max(0f, holdTime);
    }

    public float getCooldownRemaining() {
        return cooldownRemaining;
    }

    public void setCooldownRemaining(float cooldownRemaining) {
        this.cooldownRemaining = Math.max(0f, cooldownRemaining);
    }

    public Vector2 getDashVelocity() {
        return dashVelocity;
    }

    public float getDashTimeRemaining() {
        return dashTimeRemaining;
    }

    public void setDashTimeRemaining(float dashTimeRemaining) {
        this.dashTimeRemaining = dashTimeRemaining;
    }

    public Vector2 getLastDirection() {
        return lastDirection;
    }

    public void setLastDirection(Vector2 direction) {
        if (direction == null || direction.isZero()) {
            return;
        }
        this.lastDirection.set(direction).nor();
    }

}
