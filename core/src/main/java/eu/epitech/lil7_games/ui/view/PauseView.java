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
public class PauseView extends View<MenuViewModel> {

    private SaveManagerPane saveManagerPane;
    private TextButton quickLoadButton;

    public PauseView(Stage stage,
                     Skin skin,
                     MenuViewModel viewModel) {
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

        layout.add(buildPauseWindow()).minWidth(450f).growY();

        // Save manager below - only show if there are saves
        if (viewModel.hasSaveData()) {
            layout.row();
            layout.add(buildSaveManagerWindow()).growX().padTop(16f);
        }

        add(layout).expand().center();
        updateButtonStates();
    }

    @Override
    protected void setupPropertyChanges() {
        listenTo(MenuViewModel.GAME_RUNNING, Boolean.class, running -> updateButtonStates());
    }

    private Window buildPauseWindow() {
        Window window = new Window("", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        Label title = new Label("GAME PAUSED", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.5f);
        window.add(title).growX().padBottom(8f);
        window.row();

        Label subtitle = new Label("Take a moment to rest", skin);
        subtitle.setAlignment(Align.center);
        subtitle.setFontScale(0.9f);
        window.add(subtitle).growX().padBottom(32f);
        window.row();

        Table gameActions = new Table();
        gameActions.defaults().fillX().padBottom(12f);

        TextButton resumeButton = new TextButton("Resume Game", skin);
        onClick(resumeButton, viewModel::resumeGame);
        gameActions.add(resumeButton).height(50f);
        gameActions.row();

        TextButton quickSaveButton = new TextButton("Quick Save", skin);
        onClick(quickSaveButton, () -> {
            viewModel.quickSave();
            refreshSaves();
        });
        gameActions.add(quickSaveButton).height(50f);
        gameActions.row();

        quickLoadButton = new TextButton("Quick Load", skin);
        onClick(quickLoadButton, viewModel::quickLoad);
        gameActions.add(quickLoadButton).height(50f);
        gameActions.row();

        TextButton returnButton = new TextButton("Return to Main Menu", skin);
        onClick(returnButton, viewModel::returnToMainMenu);
        gameActions.add(returnButton).height(50f);

        window.add(gameActions).growX().padBottom(24f);
        window.row();

        // Audio sliders (music and SFX)
        Table audioTable = new Table();
        audioTable.defaults().padTop(6f);

        // Music
        Label musicLabel = new Label("Music", skin);
        final Label musicValue = new Label(Math.round(viewModel.getMusicVolume() * 100) + "%", skin);
        audioTable.add(musicLabel).left().growX();
        audioTable.add(musicValue).right().padLeft(8f);
        audioTable.row();
        com.badlogic.gdx.scenes.scene2d.ui.Slider musicSlider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.01f, false, skin);
        musicSlider.setValue(viewModel.getMusicVolume());
        View.onChange(musicSlider, s -> {
            viewModel.setMusicVolume(s.getValue());
            musicValue.setText(Math.round(s.getValue() * 100) + "%");
        });
        audioTable.add(musicSlider).colspan(2).growX();
        audioTable.row();

        // SFX
        Label sfxLabel = new Label("SFX", skin);
        final Label sfxValue = new Label(Math.round(viewModel.getSoundVolume() * 100) + "%", skin);
        audioTable.add(sfxLabel).left().growX();
        audioTable.add(sfxValue).right().padLeft(8f);
        audioTable.row();
        com.badlogic.gdx.scenes.scene2d.ui.Slider sfxSlider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.01f, false, skin);
        sfxSlider.setValue(viewModel.getSoundVolume());
        View.onChange(sfxSlider, s -> {
            viewModel.setSoundVolume(s.getValue());
            sfxValue.setText(Math.round(s.getValue() * 100) + "%");
        });
        audioTable.add(sfxSlider).colspan(2).growX();

        window.add(audioTable).growX().padBottom(12f);
        window.row();

        TextButton exitButton = new TextButton("Exit to Desktop", skin);
        onClick(exitButton, viewModel::quitGame);
        window.add(exitButton).height(45f).padTop(8f);

        return window;
    }

    private Window buildSaveManagerWindow() {
        Window window = new Window("Save Management", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        saveManagerPane = new SaveManagerPane(skin, viewModel);
        window.add(saveManagerPane).grow().minHeight(180f);

        return window;
    }

    private void refreshSaves() {
        if (saveManagerPane != null) {
            saveManagerPane.refresh();
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (quickLoadButton != null) {
            quickLoadButton.setDisabled(!viewModel.hasQuickSave());
        }
        if (saveManagerPane != null) {
            saveManagerPane.updateSaveButtonState();
        }
    }
}
