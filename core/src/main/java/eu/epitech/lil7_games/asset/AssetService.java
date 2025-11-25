package eu.epitech.lil7_games.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.github.tommyettinger.freetypist.FreeTypistSkinLoader;

public class AssetService implements Disposable {
    // Service d'abstraction autour d'AssetManager. Fournit des helpers synchrones (load avec finishLoading)
    // et des méthodes de queue pour un chargement asynchrone si nécessaire (update() dans un LoadingScreen).
    private final AssetManager assetManager;

    public AssetService(FileHandleResolver fileHandleResolver) {
        this.assetManager = new AssetManager(fileHandleResolver);
        this.assetManager.setLoader(TiledMap.class, new TmxMapLoader());
        this.assetManager.setLoader(Skin.class, new FreeTypistSkinLoader(fileHandleResolver));
    }

    public <T> T load(Asset<T> asset) {
        this.assetManager.load(asset.getDescriptor());
        this.assetManager.finishLoading(); // Chargement bloquant : OK pour assets légers; à surveiller si croissance.
        return this.assetManager.get(asset.getDescriptor());
    }

    public <T> void unload(Asset<T> asset) {
        this.assetManager.unload(asset.getDescriptor().fileName);
    }

    public <T> void queue(Asset<T> asset) {
        this.assetManager.load(asset.getDescriptor());
    }

    public <T> T get(Asset<T> asset) {
        return this.assetManager.get(asset.getDescriptor());
    }

    public boolean update() {
        return this.assetManager.update();
    }

    public void debugDiagnostics() {
        Gdx.app.debug("AssetService", this.assetManager.getDiagnostics());
    }

    @Override
    public void dispose() {
        this.assetManager.dispose();
    }
}

