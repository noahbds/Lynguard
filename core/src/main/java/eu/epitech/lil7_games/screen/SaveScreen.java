package eu.epitech.lil7_games.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.SkinAsset;
import eu.epitech.lil7_games.ui.model.MenuViewModel;
import eu.epitech.lil7_games.ui.view.SaveView;

public class SaveScreen extends ScreenAdapter {

    private final Lynguard game;
    private final Stage stage;
    private final Viewport viewport;
    private final Skin skin;
    private final MenuViewModel menuViewModel;

    public SaveScreen(Lynguard game) {
        this.game = game;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport, game.getBatch());
        this.skin = game.getAssetService().get(SkinAsset.DEFAULT);
        this.menuViewModel = game.getMenuViewModel();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
        game.setInputProcessors(stage);
        stage.clear();
        stage.addActor(new SaveView(stage, skin, menuViewModel));
    }

    @Override
    public void render(float delta) {
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}