package eu.epitech.lil7_games.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;

import eu.epitech.lil7_games.GameScreen;
import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.AssetService;
import eu.epitech.lil7_games.asset.SkinAsset;

public class LoadingScreen extends ScreenAdapter {

    private final Lynguard game;
    private final AssetService assetService;

    public LoadingScreen(Lynguard game) {
        this.game = game;
        this.assetService = game.getAssetService();
    }

    @Override
    public void show() {
        assetService.queue(SkinAsset.DEFAULT);
    }

    @Override
    public void render(float delta) {
        if (assetService.update()) {
            Gdx.app.debug("LoadingScreen", "Finished loading assets");
            createScreens();
            this.game.removeScreen(this);
            this.dispose();
            this.game.setScreen(SaveScreen.class);
        }
    }

    private void createScreens() {
        this.game.addScreen(new GameScreen(this.game));
        this.game.addScreen(new MenuScreen(this.game));
        this.game.addScreen(new PauseScreen(this.game));
        this.game.addScreen(new DeathScreen(this.game));
        this.game.addScreen(new SaveScreen(this.game));
    }
}
