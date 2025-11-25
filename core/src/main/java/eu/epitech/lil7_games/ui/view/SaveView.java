package eu.epitech.lil7_games.ui.view;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import eu.epitech.lil7_games.ui.model.MenuViewModel;

public class SaveView extends View<MenuViewModel> {

    public SaveView(Stage stage, Skin skin, MenuViewModel viewModel) {
        super(stage, skin, viewModel);
    }

    @Override
    protected void setupUI() {
        clearChildren();
        setFillParent(true);
        if (skin.has("alpha", Drawable.class)) {
            setBackground(skin.getDrawable("alpha"));
        }
        pad(48f);

        Table layout = new Table();
        layout.defaults().pad(24f).top().fill();
        layout.add(buildTitleWindow()).growX();
        layout.row();
        layout.add(buildSaveManagerWindow()).grow();
        layout.row();
        layout.add(buildActionsWindow()).growX();
        add(layout).expand().center();
    }

    private Window buildTitleWindow() {
        Window window = new Window("", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        Label title = new Label("LOAD OR START NEW GAME", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.5f);
        window.add(title).growX().padBottom(8f);
        window.row();

        Label subtitle = new Label("Choose your path in Lynguard", skin);
        subtitle.setAlignment(Align.center);
        subtitle.setFontScale(0.9f);
        window.add(subtitle).growX();

        return window;
    }

    private Window buildSaveManagerWindow() {
        Window window = new Window("Save Slots", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        SaveManagerPane saveManagerPane = new SaveManagerPane(skin, viewModel);
        window.add(saveManagerPane).grow().minHeight(250f);

        return window;
    }

    private Window buildActionsWindow() {
        Window window = new Window("", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        TextButton startNewGameButton = new TextButton("Start New Campaign", skin);
        onClick(startNewGameButton, viewModel::goToMainMenu);
        window.add(startNewGameButton).height(50f).growX();

        return window;
    }
}