package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Attack component tracks attack state, duration and cooldown.
 * A simple one-button melee attack used to drive animation and prevent movement/dash during its active window.
 */
public class Attack implements Component {
    public static final ComponentMapper<Attack> MAPPER = ComponentMapper.getFor(Attack.class);

    private boolean attacking;
    private float cooldownRemaining;
    // Faster chaining inside a 3-hit cycle (stage 1 & 2), longer pause after finisher (stage 3)
    private float stage1Cooldown = 0.15f;
    private float stage2Cooldown = 0.18f;
    private float stage3Cooldown = 0.50f; // pause after full cycle
    private int comboStage = 1; // 1..3
    private boolean hitRegistered = false; // prevent multiple hit applications per attack
    public boolean isAttacking() { return attacking; }
    public float getCooldownRemaining() { return cooldownRemaining; }
    public float getAttackCooldown() { return getCooldownForStage(comboStage); }
    public int getComboStage() { return comboStage; }

    public void setStage1Cooldown(float v) { this.stage1Cooldown = v; }
    public void setStage2Cooldown(float v) { this.stage2Cooldown = v; }
    public void setStage3Cooldown(float v) { this.stage3Cooldown = v; }

    public void startAttack() {
        this.attacking = true;
        this.cooldownRemaining = getCooldownForStage(comboStage); // stage-specific cooldown
        this.hitRegistered = false;
    }

    public void stopAttack() {
        this.attacking = false;
    }

    public void advanceCombo() {
        comboStage++;
        if (comboStage > 3) comboStage = 1;
    }

    public void resetComboIfIdle(float idleTimeSeconds) {
        // Placeholder for future reset logic if desired
    }

    public void update(float delta) {
        if (cooldownRemaining > 0f) {
            cooldownRemaining = Math.max(0f, cooldownRemaining - delta);
        }
        // Attack ends automatically when animation finishes (handled externally)
    }

    public boolean hasHitRegistered() { return hitRegistered; }
    public void setHitRegistered(boolean v) { this.hitRegistered = v; }

    private float getCooldownForStage(int stage) {
        return switch (stage) {
            case 1 -> stage1Cooldown;
            case 2 -> stage2Cooldown;
            case 3 -> stage3Cooldown;
            default -> stage3Cooldown;
        };
    }
}
