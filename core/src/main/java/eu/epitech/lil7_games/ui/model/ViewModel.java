package eu.epitech.lil7_games.ui.model;

import eu.epitech.lil7_games.Lynguard;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class ViewModel {
    protected final Lynguard game;
    protected final PropertyChangeSupport propertyChangeSupport;

    protected ViewModel(Lynguard game) {
        this.game = game;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public <T> PropertyChangeListener onPropertyChange(String propertyName, Class<T> propType, OnPropertyChange<T> consumer) {
        PropertyChangeListener listener = evt -> consumer.onChange(propType.cast(evt.getNewValue()));
        this.propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        return listener;
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void clearPropertyChanges() {
        for (PropertyChangeListener listener : this.propertyChangeSupport.getPropertyChangeListeners()) {
            this.propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    @FunctionalInterface
    public interface OnPropertyChange<T> {
        void onChange(T value);
    }
}
