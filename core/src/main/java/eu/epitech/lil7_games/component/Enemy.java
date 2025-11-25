package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Enemy implements Component {
    public static final ComponentMapper<Enemy> MAPPER = ComponentMapper.getFor(Enemy.class);

    public String id;

    private float contactDamage;
    private float contactCooldown;
    private float cooldownRemaining;

    public Enemy() {
        this(1f, 0.75f);
    }

    public Enemy(float contactDamage, float contactCooldown) {
        this.contactDamage = contactDamage;
        this.contactCooldown = Math.max(0f, contactCooldown);
        this.cooldownRemaining = 0f;
    }

    public float getContactDamage() {
        return contactDamage;
    }

    public void setContactDamage(float contactDamage) {
        this.contactDamage = contactDamage;
    }

    public float getContactCooldown() {
        return contactCooldown;
    }

    public void setContactCooldown(float contactCooldown) {
        this.contactCooldown = Math.max(0f, contactCooldown);
    }

    public float getCooldownRemaining() {
        return cooldownRemaining;
    }

    public void setCooldownRemaining(float cooldownRemaining) {
        this.cooldownRemaining = Math.max(0f, cooldownRemaining);
    }

    public void tickCooldown(float delta) {
        if (cooldownRemaining <= 0f) {
            return;
        }
        cooldownRemaining -= delta;
        if (cooldownRemaining < 0f) {
            cooldownRemaining = 0f;
        }
    }

    public void triggerContactCooldown() {
        this.cooldownRemaining = contactCooldown;
    }
}
