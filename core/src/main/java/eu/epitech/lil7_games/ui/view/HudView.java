package eu.epitech.lil7_games.ui.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import eu.epitech.lil7_games.ui.model.HudViewModel;

public class HudView extends View<HudViewModel> {
    private Label lifeLabel;
    private Label dashLabel;
    private Label actionLabel;
    private Image actionIndicator;
    private Table keysTable;
    private final Texture keyTexture;

    public HudView(Stage stage, Skin skin, HudViewModel viewModel) {
        super(stage, skin, viewModel);
        keyTexture = new Texture(com.badlogic.gdx.Gdx.files.internal("maps/items/item403.png"));
    }

    @Override
    protected void setupUI() {
        setFillParent(true);
        bottom();

        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0f, 0f, 0f, 1f);
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();
        TextureRegionDrawable bgDrawable = new TextureRegionDrawable(new TextureRegion(bgTexture));

        Table container = new Table();
        container.setBackground(bgDrawable);
        container.pad(10);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        // Life
        lifeLabel = new Label("Life: ", skin);
        container.add(lifeLabel).expandX().left();
        
        // Dash
        dashLabel = new Label("Dash: ", skin);
        container.add(dashLabel).expandX().center();

        // Keys
        keysTable = new Table();
        container.add(keysTable).expandX().center();
        
        // Action
        Table actionTable = new Table();
        actionLabel = new Label("Action: ", skin);
        actionTable.add(actionLabel);
        actionIndicator = new Image(whiteTexture);
        actionIndicator.setColor(Color.RED);
        actionTable.add(actionIndicator).size(20);
        container.add(actionTable).expandX().right();

        add(container).growX();
    }

    @Override
    protected void setupPropertyChanges() {
        listenTo("life", Float.class, this::updateLife);
        listenTo("maxLife", Float.class, this::updateMaxLife);
        listenTo("dashCooldown", Float.class, this::updateDashCooldown);
        listenTo("maxDashCooldown", Float.class, this::updateMaxDashCooldown);
        listenTo("isActionPossible", Boolean.class, this::updateActionIndicator);
        listenTo("keys", java.util.List.class, this::updateKeys);
        listenTo("actionText", String.class, this::updateActionText);
    }

    private void updateKeys(java.util.List<String> keys) {
        keysTable.clear();
        if (keys != null) {
            for (String key : keys) {
                Image keyImage = new Image(keyTexture);
                keyImage.setName(key);
                keysTable.add(keyImage).size(32).pad(5);
            }
        }
    }

    private void updateActionText(String text) {
        if (text != null && !text.isEmpty()) {
            actionLabel.setText(text);
        } else {
            actionLabel.setText("Action: ");
        }
    }

    private void updateLife(Float life) {
        updateLifeLabel();
    }

    private void updateMaxLife(Float maxLife) {
        updateLifeLabel();
    }

    private void updateLifeLabel() {
        lifeLabel.setText("Life: " + (int)viewModel.getLife() + " / " + (int)viewModel.getMaxLife());
    }

    private void updateDashCooldown(Float cooldown) {
        updateDashLabel();
    }

    private void updateMaxDashCooldown(Float maxCooldown) {
        updateDashLabel();
    }

    private void updateDashLabel() {
        if (viewModel.getDashCooldown() <= 0) {
            dashLabel.setText("Dash: READY");
            dashLabel.setColor(Color.GREEN);
        } else {
            dashLabel.setText(String.format("Dash: %.1fs", viewModel.getDashCooldown()));
            dashLabel.setColor(Color.YELLOW);
        }
    }

    private void updateActionIndicator(Boolean isPossible) {
        if (isPossible) {
            actionIndicator.setColor(Color.GREEN);
        } else {
            actionIndicator.setColor(Color.RED);
        }
    }
}
