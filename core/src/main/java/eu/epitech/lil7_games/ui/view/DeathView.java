package eu.epitech.lil7_games.ui.view;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import eu.epitech.lil7_games.ui.model.MenuViewModel;

public class DeathView extends View<MenuViewModel> {

    public DeathView(Stage stage, Skin skin, MenuViewModel viewModel) {
        super(stage, skin, viewModel);
    }

    @Override
    protected void setupUI() {
        clearChildren();
        setFillParent(true);
        pad(48f);
        if (skin.has("alpha", Drawable.class)) {
            setBackground(skin.getDrawable("alpha"));
        }

        Table layout = new Table();
        layout.defaults().pad(24f).top().fill();

        // Main death screen
        layout.add(buildDeathWindow()).minWidth(450f).growY();

        add(layout).expand().center();
    }

    private Window buildDeathWindow() {
        Window window = new Window("", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        // Title section
        Label title = new Label("YOU DIED", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.5f);
        window.add(title).growX().padBottom(8f);
        window.row();

        Label subtitle = new Label("But the adventure can continue...", skin);
        subtitle.setAlignment(Align.center);
        subtitle.setFontScale(0.9f);
        window.add(subtitle).growX().padBottom(32f);
        window.row();

        // Game actions section
        Table gameActions = new Table();
        gameActions.defaults().fillX().padBottom(12f);

        TextButton restartButton = new TextButton("Restart Level", skin);
        onClick(restartButton, viewModel::restartGame);
        gameActions.add(restartButton).height(50f);
        gameActions.row();

        TextButton loadLastSaveButton = new TextButton("Load Latest Save", skin);
        onClick(loadLastSaveButton, viewModel::loadLatestSave);
        gameActions.add(loadLastSaveButton).height(50f);
        gameActions.row();

        TextButton loadSavesButton = new TextButton("Load from Saves", skin);
        onClick(loadSavesButton, this::showLoadSavesDialog);
        gameActions.add(loadSavesButton).height(50f);
        gameActions.row();

        TextButton menuButton = new TextButton("Return to Main Menu", skin);
        onClick(menuButton, viewModel::returnToMainMenu);
        gameActions.add(menuButton).height(50f);

        window.add(gameActions).growX().padBottom(24f);
        window.row();

        // Exit section
        TextButton exitButton = new TextButton("Exit to Desktop", skin);
        onClick(exitButton, viewModel::quitGame);
        window.add(exitButton).height(45f).padTop(8f);

        return window;
    }

    private void showLoadSavesDialog() {
        Dialog dialog = new Dialog("Load Saves", skin);
        SaveManagerPane pane = new SaveManagerPane(skin, viewModel);
        pane.setSavingEnabled(false);
        dialog.getContentTable().add(pane).grow();
        dialog.button("Close");
        dialog.show(stage);
    }
}
