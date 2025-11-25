package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;

import eu.epitech.lil7_games.component.NoGravity;
import eu.epitech.lil7_games.component.NoGravityCooldown;
import eu.epitech.lil7_games.component.Player;

public class NoGravityHudSystem extends EntitySystem {
    private final Stage stage;
    private final BitmapFont font;
    private Label label;

    public NoGravityHudSystem(Stage stage) {
        this.stage = stage;
        this.font = new BitmapFont();
        this.label = null;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        if (label == null) {
            label = new Label("", new LabelStyle(font, Color.CYAN));
            label.setFontScale(1f);
            stage.addActor(label);
            label.setVisible(false);
        }
    }

    @Override
    public void update(float deltaTime) {
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(Player.class).get());
        if (players.size() == 0) {
            if (label != null) label.setVisible(false);
            return;
        }

        Entity p = players.first();
        NoGravity ng = NoGravity.MAPPER.get(p);
        NoGravityCooldown cd = NoGravityCooldown.MAPPER.get(p);

        if (ng != null) {
            String txt = String.format("Anti-grav: %.1fs", ng.getRemainingSeconds());
            label.setText(txt);
            label.setVisible(true);
        } else if (cd != null && cd.getRemainingSeconds() > 0f) {
            String txt = String.format("Anti-grav CD: %.1fs", cd.getRemainingSeconds());
            label.setText(txt);
            label.setVisible(true);
        } else {
            label.setVisible(false);
        }

        float x = 8f;
        float y = stage.getViewport().getWorldHeight() - 12f;
        label.setPosition(x, y);
    }
}
