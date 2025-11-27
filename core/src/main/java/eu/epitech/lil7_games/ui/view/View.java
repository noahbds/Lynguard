package eu.epitech.lil7_games.ui.view;

import java.beans.PropertyChangeListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import eu.epitech.lil7_games.ui.model.ViewModel;

public abstract class View<T extends ViewModel> extends Table {
    protected final Stage stage;
    protected final Skin skin;
    protected final T viewModel;
    private final Array<PropertyChangeListener> propertyListeners = new Array<>();


    protected View(Stage stage, Skin skin, T viewModel) {
        this.stage = stage;
        this.skin = skin;
        this.viewModel = viewModel;
        setupUI();
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        if (stage == null) {
            detachListeners();
        }
        super.setStage(stage);
        if (stage != null) {
            detachListeners();
            setupPropertyChanges();
        }
    }

    protected abstract void setupUI();

    protected void setupPropertyChanges() {}

    protected <V> void listenTo(String propertyName, Class<V> type, ViewModel.OnPropertyChange<V> consumer) {
        PropertyChangeListener listener = viewModel.onPropertyChange(propertyName, type, consumer);
        propertyListeners.add(listener);
    }

    private void detachListeners() {
        for (PropertyChangeListener listener : propertyListeners) {
            viewModel.removePropertyChangeListener(listener);
        }
        propertyListeners.clear();
    }

    public static void onClick(Actor actor, OnEventConsumer consumer) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                consumer.onEvent();
            }
        });
    }

    public static <T extends Actor> void onEnter(T actor, OnActorEvent<T> consumer) {
        actor.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                consumer.onEvent(actor);
            }
        });
    }

    public static <T extends Actor> void onChange(T actor, OnActorEvent<T> consumer) {
        actor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor eventActor) {
                consumer.onEvent(actor);
            }
        });
    }

    @FunctionalInterface
    public interface OnEventConsumer {
        void onEvent();
    }

    @FunctionalInterface
    public interface OnActorEvent<T extends Actor> {
        void onEvent(T actor);
    }
}
