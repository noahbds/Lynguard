package eu.epitech.lil7_games.ui.model;

import eu.epitech.lil7_games.Lynguard;

public class HudViewModel extends ViewModel {
    private float life;
    private float maxLife;
    private float dashCooldown;
    private float maxDashCooldown;
    private boolean isDashReady;
    private boolean isActionPossible;
    private String actionText;
    private java.util.List<String> keys = new java.util.ArrayList<>();

    public HudViewModel(Lynguard game) {
        super(game);
    }

    public java.util.List<String> getKeys() {
        return keys;
    }

    public void addKey(String key) {
        java.util.List<String> oldKeys = new java.util.ArrayList<>(this.keys);
        this.keys.add(key);
        propertyChangeSupport.firePropertyChange("keys", oldKeys, this.keys);
    }

    public void removeKey(String key) {
        java.util.List<String> oldKeys = new java.util.ArrayList<>(this.keys);
        if (this.keys.remove(key)) {
            propertyChangeSupport.firePropertyChange("keys", oldKeys, this.keys);
        }
    }
    
    public void clearKeys() {
        java.util.List<String> oldKeys = new java.util.ArrayList<>(this.keys);
        if (!this.keys.isEmpty()) {
            this.keys.clear();
            propertyChangeSupport.firePropertyChange("keys", oldKeys, this.keys);
        }
    }

    public void setKeys(java.util.List<String> keys) {
        java.util.List<String> oldKeys = new java.util.ArrayList<>(this.keys);
        this.keys = new java.util.ArrayList<>(keys);
        propertyChangeSupport.firePropertyChange("keys", oldKeys, this.keys);
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        String oldActionText = this.actionText;
        this.actionText = actionText;
        propertyChangeSupport.firePropertyChange("actionText", oldActionText, actionText);
    }

    public float getLife() {
        return life;
    }

    public void setLife(float life) {
        float oldLife = this.life;
        this.life = life;
        propertyChangeSupport.firePropertyChange("life", oldLife, life);
    }

    public float getMaxLife() {
        return maxLife;
    }

    public void setMaxLife(float maxLife) {
        float oldMaxLife = this.maxLife;
        this.maxLife = maxLife;
        propertyChangeSupport.firePropertyChange("maxLife", oldMaxLife, maxLife);
    }

    public float getDashCooldown() {
        return dashCooldown;
    }

    public void setDashCooldown(float dashCooldown) {
        float oldDashCooldown = this.dashCooldown;
        this.dashCooldown = dashCooldown;
        propertyChangeSupport.firePropertyChange("dashCooldown", oldDashCooldown, dashCooldown);
    }

    public float getMaxDashCooldown() {
        return maxDashCooldown;
    }

    public void setMaxDashCooldown(float maxDashCooldown) {
        float oldMaxDashCooldown = this.maxDashCooldown;
        this.maxDashCooldown = maxDashCooldown;
        propertyChangeSupport.firePropertyChange("maxDashCooldown", oldMaxDashCooldown, maxDashCooldown);
    }

    public boolean isDashReady() {
        return isDashReady;
    }

    public void setDashReady(boolean dashReady) {
        boolean oldDashReady = this.isDashReady;
        this.isDashReady = dashReady;
        propertyChangeSupport.firePropertyChange("isDashReady", oldDashReady, dashReady);
    }

    public boolean isActionPossible() {
        return isActionPossible;
    }

    public void setActionPossible(boolean actionPossible) {
        boolean oldActionPossible = this.isActionPossible;
        this.isActionPossible = actionPossible;
        propertyChangeSupport.firePropertyChange("isActionPossible", oldActionPossible, actionPossible);
    }
}
