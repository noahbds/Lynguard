package eu.epitech.lil7_games.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.MusicAsset;
import eu.epitech.lil7_games.asset.SkinAsset;
import eu.epitech.lil7_games.ui.view.MenuView;

public class MenuScreen extends ScreenAdapter {
    // Ecran du menu principal : instancie l'UI (MenuView) et lance la musique d'ambiance.

    private final Lynguard game;
    private final Stage stage;
    private final Skin skin;
    private final Viewport uiViewport;
    public MenuScreen(Lynguard game) {
        this.game = game;
        this.uiViewport = new ScreenViewport();
        this.stage = new Stage(uiViewport, game.getBatch());
        this.skin = game.getAssetService().get(SkinAsset.DEFAULT);
    }

    @Override
    public void resize(int width, int height) {
        uiViewport.update(width, height, true);
    }

    @Override
    public void show() {
        this.game.setInputProcessors(stage);

        this.stage.clear();
        this.stage.addActor(new MenuView(stage, skin, game.getMenuViewModel()));
        this.game.getAudioService().playMusic(MusicAsset.TOWN); // Démarrage musique menu.
    }

    @Override
    public void hide() {
        this.stage.clear();
        this.game.getAudioService().stopMusic(MusicAsset.TOWN); // Arrêt musique lors de sortie.
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
