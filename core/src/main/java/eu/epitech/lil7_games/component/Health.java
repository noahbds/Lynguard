package eu.epitech.lil7_games.component;

import java.awt.*;

public class Health extends Component {
    //public static final ComponentMapper<Health> MAPPER = ComponentMapper.getFor(Health.class);
    

    private int currentHealth;
    private int maxHealth;

    public Health(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = Math.max(0, Math.min(currentHealth, maxHealth));
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (this.currentHealth > maxHealth) {
            this.currentHealth = maxHealth;
        }
    }
}
