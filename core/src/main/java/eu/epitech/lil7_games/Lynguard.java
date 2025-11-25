package eu.epitech.lil7_games;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import eu.epitech.lil7_games.asset.AssetService;
import eu.epitech.lil7_games.audio.AudioService;
import eu.epitech.lil7_games.screen.LoadingScreen;
import eu.epitech.lil7_games.ui.model.GameViewModel;
import eu.epitech.lil7_games.ui.model.MenuViewModel;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */

public class Lynguard extends Game {
    // Point d'entrée LibGDX (Game) : gère caméra, viewport, asset service, audio et cache d'écrans.
    public static final float WORLD_WIDTH = 16f;
    public static final float WORLD_HEIGHT = 9f;
    public static final float UNIT_SCALE = 1f/40f;

    private Batch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private AssetService assetService;
    private GLProfiler glProfiler;
    private InputMultiplexer inputMultiplexer;
    private FPSLogger fpsLogger;
    private AudioService audioService;
    private MenuViewModel menuViewModel;
    private GameViewModel gameViewModel;
    private boolean gameRunning;

    // Cache des screens pour éviter re-créations coûteuses (pas de DI poussée ici, simple map). Attention :
    // bien appeler dispose() sur chaque screen lors du dispose du jeu pour libérer ressources.
    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        inputMultiplexer = new InputMultiplexer();
        Gdx.input.setInputProcessor(inputMultiplexer);

        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        this.assetService = new AssetService(new InternalFileHandleResolver());
        this.audioService = new AudioService(assetService);
        this.menuViewModel = new MenuViewModel(this);
        this.gameViewModel = new GameViewModel(this);
        this.glProfiler = new GLProfiler(Gdx.graphics); // Profilage GL basique (draw calls).
        this.glProfiler.enable(); // Peut être désactivé en production pour éviter overhead.
        this.fpsLogger = new FPSLogger();

        addScreen(new LoadingScreen(this));
        setScreen(LoadingScreen.class);
    }

    public void addScreen(Screen screen) {
        screenCache.put(screen.getClass(), screen);
    }

    public void setScreen(Class<? extends Screen> screenClass) {
        Screen screen = screenCache.get(screenClass);
        if (screen == null) {
            throw new GdxRuntimeException("Screen " + screenClass.getSimpleName() + " not found in cache");
        }
        super.setScreen(screen);
    }

    public void removeScreen(Screen screen) {
        screenCache.remove(screen.getClass());
    }

    public <T extends Screen> T getScreen(Class<T> screenClass) {
        return screenClass.cast(screenCache.get(screenClass));
    }

    @Override
    public void render() {
        // Cycle principal : clear, render screen courant, mise à jour titre debug (draw calls + FPS Logger).
        glProfiler.reset();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render();

        Gdx.graphics.setTitle("Lynguard, Draw calls : " + glProfiler.getDrawCalls());
        fpsLogger.log();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        super.resize(width, height);
    }

    /**
     * Cleans up all game resources and services.
     */
    @Override
    public void dispose() {
        // Libération : on s'assure de disposer chaque Screen avant de vider les services partagés.
        for (Screen screen : screenCache.values()) {
            screen.dispose();
        }
        screenCache.clear();

        batch.dispose();
        assetService.debugDiagnostics();
        assetService.dispose();
    }

    public Batch getBatch() {
        return batch;
    }

    public AssetService getAssetService() {
        return assetService;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void setInputProcessors(InputProcessor... processors) {
        // Remplace complètement la pile de processors (pas d'empilement). Convenable pour ce projet simple.
        inputMultiplexer.clear();
        if (processors == null) return;

        for (InputProcessor processor : processors) {
            inputMultiplexer.addProcessor(processor);
        }
    }

    public AudioService getAudioService() {
        return audioService;
    }

    public MenuViewModel getMenuViewModel() {
        return menuViewModel;
    }

    public GameViewModel getGameViewModel() {
        return gameViewModel;
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void setGameRunning(boolean gameRunning) {
        // Indicateur partagé pour savoir si une partie est active (utilisé pour reset GameScreen à hide()).
        this.gameRunning = gameRunning;
    }
}
