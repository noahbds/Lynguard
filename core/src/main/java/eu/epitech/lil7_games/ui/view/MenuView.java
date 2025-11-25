package eu.epitech.lil7_games.ui.view;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import eu.epitech.lil7_games.ui.model.MenuViewModel;

public class MenuView extends View<MenuViewModel> {

    private Container<Window> pauseContainer;
    private Window pauseMenuWindow;
    private SaveManagerPane saveManagerPane;
    private TextButton continueButton;
    private TextButton loadLatestButton;
    private TextButton quickSaveButton;
    private TextButton quickLoadButton;
    private TextButton resumeButton;
    private TextButton returnMenuButton;

    public MenuView(Stage stage, Skin skin, MenuViewModel viewModel) {
        super(stage, skin, viewModel);
    }

    @Override
    protected void setupUI() {
        clearChildren();
        setFillParent(true);
        if (skin.has("alpha", Drawable.class)) {
            setBackground(skin.getDrawable("alpha"));
        }
        pad(32f);

        Table layout = new Table();
        layout.defaults().pad(16f).top().fill();

        layout.add(buildMainMenuWindow()).minWidth(400f).growY();

        pauseMenuWindow = buildPauseMenuWindow();
        pauseContainer = new Container<>();
        pauseContainer.fill();
        layout.add(pauseContainer).minWidth(400f).growY();
        layout.row();

        if (viewModel.hasSaveData()) {
            layout.add(buildSaveMenuWindow()).colspan(2).growX().padTop(24f);
        }

        add(layout).expand().center();

        applyGameRunningState(viewModel.isGameRunning());
        updateSaveActionsEnabled();
    }

    @Override
    protected void setupPropertyChanges() {
        listenTo(MenuViewModel.GAME_RUNNING, Boolean.class, this::applyGameRunningState);
    }

    private Window buildMainMenuWindow() {
        Window window = new Window("", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        Label title = new Label("LYNGUARD", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.5f);
        window.add(title).growX().padBottom(8f);
        window.row();

        Label subtitle = new Label("A Medieval Fantasy Adventure", skin);
        subtitle.setAlignment(Align.center);
        subtitle.setFontScale(0.9f);
        window.add(subtitle).growX().padBottom(32f);
        window.row();

        Table gameActions = new Table();
        gameActions.defaults().fillX().padBottom(12f);

        TextButton startButton = new TextButton("Start New Campaign", skin);
        onClick(startButton, viewModel::startGame);
        gameActions.add(startButton).height(50f);
        gameActions.row();

        continueButton = new TextButton("Continue Campaign", skin);
        continueButton.setDisabled(!viewModel.hasSaveData());
        bindEnabledAction(continueButton, viewModel::continueGame);
        gameActions.add(continueButton).height(50f);
        gameActions.row();

        loadLatestButton = new TextButton("Load Latest Save", skin);
        loadLatestButton.setDisabled(!viewModel.hasSaveData());
        bindEnabledAction(loadLatestButton, () -> {
            String latestSlot = viewModel.getLatestSlotName();
            if (latestSlot != null) {
                viewModel.loadSlot(latestSlot);
            }
        });
        gameActions.add(loadLatestButton).height(50f);

        window.add(gameActions).growX().padBottom(24f);
        window.row();

        Table settingsActions = new Table();
        settingsActions.defaults().fillX().padBottom(8f);

        ImageTextButton settingsButton = new ImageTextButton("Settings", skin);
        onClick(settingsButton, viewModel::openSettings);
        settingsActions.add(settingsButton).height(45f);
        settingsActions.row();

        window.add(settingsActions).growX().padBottom(24f);
        window.row();

        window.add(buildAudioControls()).growX().padBottom(24f);
        window.row();

        TextButton exitButton = new TextButton("Exit Game", skin);
        onClick(exitButton, viewModel::quitGame);
        window.add(exitButton).height(45f).padTop(8f);

        return window;
    }

    private Table buildAudioControls() {
        Table audioTable = new Table();
        audioTable.defaults().fillX().padTop(12f);

        Label sectionTitle = new Label("Audio Settings", skin);
        sectionTitle.setAlignment(Align.center);
        sectionTitle.setFontScale(1.1f);
        audioTable.add(sectionTitle).growX().padBottom(16f);
        audioTable.row();

        audioTable.add(buildSliderRow("Music Volume", viewModel.getMusicVolume(),
            value -> viewModel.setMusicVolume(value))).growX();
        audioTable.row();

        audioTable.add(buildSliderRow("Sound Effects", viewModel.getSoundVolume(),
            value -> viewModel.setSoundVolume(value))).growX();

        return audioTable;
    }

        private Table buildSliderRow(String labelText, float initialValue, ValueConsumer consumer) {
        Table row = new Table();
        row.defaults().padTop(4f);

        Table header = new Table();
        header.add(new Label(labelText, skin)).left().growX();
        final Label valueLabel = new Label(formatPercent(initialValue), skin);
        header.add(valueLabel).padLeft(8f).align(Align.right);
        row.add(header).growX();
        row.row();

        final Slider slider = new Slider(0f, 1f, 0.01f, false, skin);
        slider.setValue(initialValue);
        row.add(slider).growX();

        onChange(slider, s -> {
            consumer.onValue(s.getValue());
            valueLabel.setText(formatPercent(s.getValue()));
        });

        return row;
    }

    private Window buildPauseMenuWindow() {
        Window window = new Window("", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        Label title = new Label("GAME PAUSED", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.5f);
        window.add(title).growX().padBottom(8f);
        window.row();

        Label subtitle = new Label("Adventure temporarily suspended", skin);
        subtitle.setAlignment(Align.center);
        subtitle.setFontScale(0.9f);
        window.add(subtitle).growX().padBottom(32f);
        window.row();

        Table gameActions = new Table();
        gameActions.defaults().fillX().padBottom(12f);

        resumeButton = new TextButton("Resume Game", skin);
        bindEnabledAction(resumeButton, viewModel::resumeGame);
        gameActions.add(resumeButton).height(50f);
        gameActions.row();

        quickSaveButton = new TextButton("Quick Save", skin);
        bindEnabledAction(quickSaveButton, () -> {
            viewModel.quickSave();
            refreshSavesPane();
        });
        gameActions.add(quickSaveButton).height(50f);
        gameActions.row();

        quickLoadButton = new TextButton("Quick Load", skin);
        bindEnabledAction(quickLoadButton, viewModel::quickLoad);
        gameActions.add(quickLoadButton).height(50f);
        gameActions.row();

        returnMenuButton = new TextButton("Return to Main Menu", skin);
        bindEnabledAction(returnMenuButton, viewModel::returnToMainMenu);
        gameActions.add(returnMenuButton).height(50f);

        window.add(gameActions).growX().padBottom(24f);
        window.row();

        TextButton quitDesktopButton = new TextButton("Exit to Desktop", skin);
        onClick(quitDesktopButton, viewModel::quitGame);
        window.add(quitDesktopButton).height(45f).padTop(8f);

        return window;
    }

    private Window buildSaveMenuWindow() {
        Window window = new Window("Save Management", skin);
        window.defaults().padTop(16f).fillX().padLeft(24f).padRight(24f);

        saveManagerPane = new SaveManagerPane(skin, viewModel);
        window.add(saveManagerPane).grow().minHeight(200f);

        return window;
    }

    private void applyGameRunningState(boolean running) {
        if (pauseContainer != null) {
            pauseContainer.setActor(running ? pauseMenuWindow : null);
        }
        if (resumeButton != null) {
            resumeButton.setDisabled(!running);
        }
        if (quickSaveButton != null) {
            quickSaveButton.setDisabled(!running);
        }
        if (returnMenuButton != null) {
            returnMenuButton.setDisabled(!running);
        }
        if (saveManagerPane != null) {
            saveManagerPane.updateSaveButtonState();
        }
        updateSaveActionsEnabled();
    }

    private void updateSaveActionsEnabled() {
        boolean hasSaves = viewModel.hasSaveData();
        if (continueButton != null) {
            continueButton.setDisabled(!hasSaves);
        }
        if (loadLatestButton != null) {
            loadLatestButton.setDisabled(!hasSaves);
        }
        if (quickLoadButton != null) {
            boolean quickLoadEnabled = viewModel.isGameRunning() && viewModel.hasQuickSave();
            quickLoadButton.setDisabled(!quickLoadEnabled);
        }
    }

    private void refreshSavesPane() {
        if (saveManagerPane != null) {
            saveManagerPane.refresh();
        }
        updateSaveActionsEnabled();
    }

    private void bindEnabledAction(final TextButton button, Runnable action) {
        onClick(button, () -> {
            if (!button.isDisabled()) {
                action.run();
            }
        });
    }

    private String formatPercent(float value) {
        return Math.round(value * 100f) + "%";
    }

    @FunctionalInterface
    private interface ValueConsumer {
        void onValue(float value);
    }
}
