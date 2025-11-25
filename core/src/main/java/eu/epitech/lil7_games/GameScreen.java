package eu.epitech.lil7_games;

import java.util.function.Consumer;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import eu.epitech.lil7_games.asset.AssetService;
import eu.epitech.lil7_games.asset.MapAsset;
import eu.epitech.lil7_games.asset.SkinAsset;
import eu.epitech.lil7_games.audio.AudioService;
import eu.epitech.lil7_games.component.Animation2D;
import eu.epitech.lil7_games.component.AnimationSystem;
import eu.epitech.lil7_games.component.Collider;
import eu.epitech.lil7_games.component.Dash;
import eu.epitech.lil7_games.component.Dying;
import eu.epitech.lil7_games.component.Enemy;
import eu.epitech.lil7_games.component.Facing;
import eu.epitech.lil7_games.component.Flip;
import eu.epitech.lil7_games.component.Graphic;
import eu.epitech.lil7_games.component.Life;
import eu.epitech.lil7_games.component.NoGravity;
import eu.epitech.lil7_games.component.Patrol;
import eu.epitech.lil7_games.component.Physics;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;
import eu.epitech.lil7_games.component.Velocity;
import eu.epitech.lil7_games.save.SaveData;
import eu.epitech.lil7_games.screen.DeathScreen;
import eu.epitech.lil7_games.screen.PauseScreen;
import eu.epitech.lil7_games.system.AnimatedTileSystem;
import eu.epitech.lil7_games.system.CameraSystem;
import eu.epitech.lil7_games.system.DyingSystem;
import eu.epitech.lil7_games.system.EnemySystem;
import eu.epitech.lil7_games.system.HazardSystem;
import eu.epitech.lil7_games.system.InteractionSystem;
import eu.epitech.lil7_games.system.InvulnerableSystem;
import eu.epitech.lil7_games.system.LevelTransitionSystem;
import eu.epitech.lil7_games.system.LevelTransitionSystem.LevelTransitionEvent;
import eu.epitech.lil7_games.system.LevelTransitionSystem.TransitionType;
import eu.epitech.lil7_games.system.NoGravityHudSystem;
import eu.epitech.lil7_games.system.PlayerSystem;
import eu.epitech.lil7_games.system.RenderSystem;
import eu.epitech.lil7_games.tiled.TiledAshleyConfigurator;
import eu.epitech.lil7_games.tiled.TiledService;
import eu.epitech.lil7_games.ui.model.HudViewModel;
import eu.epitech.lil7_games.ui.view.HudView;


public class GameScreen extends ScreenAdapter {
    /*
     * Ecran principal de jeu.
     * Contient l'initialisation de la carte Tiled, la gestion des systèmes ECS (Ashley) et la logique
     * runtime comme le spawn d'ennemis, transitions de niveaux, sauvegarde auto et HUD.
     * Règle importante : éviter de surcharger ce Screen avec de la logique métier spécifique d'entités;
     * déplacer cela dans des Systems dédiés si ça devient trop volumineux.
     */
    // Dimensions logiques de slime (sprite 28x28) en unités monde
    private static final float ENEMY_WIDTH = 20f * Lynguard.UNIT_SCALE;
    private static final float ENEMY_HEIGHT = 20f * Lynguard.UNIT_SCALE;
    private static final float DEFAULT_ENEMY_SPEED = 1.25f;
    private static final float ENEMY_COLLIDER_SCALE_X = 0.75f;
    private static final float ENEMY_COLLIDER_SCALE_Y = 0.8f;

    private final Lynguard game;
    private final Batch batch;
    private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final Engine engine;
    private final TiledService tiledService;
    private final Stage stage;
    private final Viewport uiViewport;
    private final TiledAshleyConfigurator tiledAshleyConfigurator;
    private final AudioService audioService;
    private final AnimatedTileSystem animatedTileSystem;
    private final PlayerSystem playerSystem;
    private final EnemySystem enemySystem;
    private final HazardSystem hazardSystem;
    private final CameraSystem cameraSystem;
    private final LevelTransitionSystem levelTransitionSystem;
    private final AnimationSystem animationSystem;
    private final RenderSystem renderSystem;
    private final InvulnerableSystem invulnerableSystem;
    private final InteractionSystem interactionSystem;
    private Texture playerTexture;
    private Texture enemyTexture;
    private Entity playerEntity;
    private final Array<Entity> enemyEntities;
    private TiledMap currentMap;
    private MapAsset currentLevel;
    private boolean pendingRespawn;
    private boolean initialized;
    private SaveData saveDataToLoad;
    private final HudViewModel hudViewModel;
    private final HudView hudView;

    private java.util.List<String> persistentKeys = new java.util.ArrayList<>();
    private final java.util.Map<String, java.util.Set<String>> deadEnemiesPerLevel = new java.util.HashMap<>();
    private float autoSaveTimer = 0f;

    public GameScreen(Lynguard game) {
        this.game = game;
        this.assetService = game.getAssetService();
        this.tiledService = new TiledService(this.assetService);
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.engine = new Engine();
        this.uiViewport = new ScreenViewport();
        this.stage = new Stage(uiViewport, game.getBatch());
        this.enemyEntities = new Array<>();
        
        this.hudViewModel = new HudViewModel(game);
        this.hudView = new HudView(stage, assetService.get(SkinAsset.DEFAULT), hudViewModel);
        this.stage.addActor(hudView);

        this.audioService = game.getAudioService();
        this.tiledAshleyConfigurator = new TiledAshleyConfigurator(this.engine, this.assetService);
        this.animatedTileSystem = new AnimatedTileSystem();
        this.playerSystem = new PlayerSystem();
    this.playerSystem.setAudioService(this.audioService);
        this.enemySystem = new EnemySystem();
        this.hazardSystem = new HazardSystem();
        this.hazardSystem.setDeathListener(this::handlePlayerDeath);
        this.cameraSystem = new CameraSystem(this.camera);
        this.levelTransitionSystem = new LevelTransitionSystem();
        this.animationSystem = new AnimationSystem(this.assetService);
        this.renderSystem = new RenderSystem(this.batch, this.viewport, this.camera);
        this.invulnerableSystem = new InvulnerableSystem();
        this.interactionSystem = new InteractionSystem(this.hudViewModel, this.levelTransitionSystem);

        this.engine.addSystem(this.playerSystem);
        this.engine.addSystem(this.enemySystem);
        this.engine.addSystem(this.hazardSystem);
        this.engine.addSystem(this.cameraSystem);
        this.engine.addSystem(this.levelTransitionSystem);
        this.engine.addSystem(this.animationSystem);
        this.engine.addSystem(this.renderSystem);
        this.engine.addSystem(this.invulnerableSystem);
        this.engine.addSystem(this.interactionSystem);
        this.engine.addSystem(new eu.epitech.lil7_games.system.LifeSystem(game.getGameViewModel()));
        this.engine.addSystem(new eu.epitech.lil7_games.system.DamagedSystem(game.getGameViewModel()));
        DyingSystem dyingSystem = new DyingSystem();
        dyingSystem.setDeathListener(this::onEntityDeath);
        this.engine.addSystem(dyingSystem);
        this.engine.addSystem(this.animatedTileSystem);
    this.engine.addSystem(new NoGravityHudSystem(this.stage));
    }

    @Override
    public void resize(int width, int height){
        super.resize(width, height);
        this.viewport.update(width, height, true);
        this.uiViewport.update(width, height, true);
    }

    @Override
    public void show(){
        game.setInputProcessors(stage);
        if (!initialized) {
            initializeWorld();
            initialized = true;
        }
        if (saveDataToLoad != null) {
            loadSaveData(saveDataToLoad);
            saveDataToLoad = null;
        }
    }

    private void initializeWorld() {
        // Réinitialise l'état persistant (clés du joueur, morts ennemies) et configure la chaîne de consommateurs
        // appelée à chaque changement de carte (renderer -> playerSystem -> camera -> hazard ...)
        this.persistentKeys.clear();
        Consumer<TiledMap> renderConsumer = this.renderSystem::setMap;
        Consumer<TiledMap> playerConsumer = this.playerSystem::setMap;
        Consumer<TiledMap> cameraConsumer = this.cameraSystem::setMap;
        Consumer<TiledMap> hazardConsumer = this.hazardSystem::setMap;
        Consumer<TiledMap> transitionConsumer = this.levelTransitionSystem::setMap;
        Consumer<TiledMap> audioConsumer = this.audioService::setMap;
        Consumer<TiledMap> interactionConsumer = this.interactionSystem::setMap;

        this.tiledService.setMapChangeConsumer(renderConsumer
            .andThen(playerConsumer)
            .andThen(cameraConsumer)
            .andThen(hazardConsumer)
            .andThen(transitionConsumer)
            .andThen(audioConsumer)
            .andThen(interactionConsumer));

        this.levelTransitionSystem.setTransitionListener(this::onLevelTransition);
        this.tiledService.setLoadObjectConsumer(this.tiledAshleyConfigurator::onLoadObject);

        if (saveDataToLoad != null) {
            loadSaveData(saveDataToLoad);
            saveDataToLoad = null;
        } else {
            TiledMap tiledMap = this.tiledService.loadMap(MapAsset.LEVEL_0);
            this.tiledService.setMap(tiledMap);
            this.currentMap = tiledMap;
            this.currentLevel = MapAsset.LEVEL_0;

            createPlayerAtMapSpawn(tiledMap, MapAsset.LEVEL_0);
            spawnEnemies(tiledMap);
        }
    }

    private void onEntityDeath(Entity entity) {
        Enemy enemy = Enemy.MAPPER.get(entity);
        if (enemy != null && enemy.id != null && currentLevel != null) {
            String levelName = currentLevel.name();
            deadEnemiesPerLevel.computeIfAbsent(levelName, k -> new java.util.HashSet<>()).add(enemy.id);
        }
    }

    private void createPlayerTexture() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fill();
        playerTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createEnemyTexture() {
        if (enemyTexture != null) {
            return;
        }

        Pixmap pixmap = new Pixmap(28, 28, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.FOREST);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.drawRectangle(0, 0, pixmap.getWidth(), pixmap.getHeight());
        enemyTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createPlayerAtMapSpawn(com.badlogic.gdx.maps.tiled.TiledMap map, MapAsset mapAsset) {
        this.currentMap = map;
        this.currentLevel = mapAsset;
        com.badlogic.gdx.math.Vector2 spawnPosition = findPlayerSpawn(map, mapAsset);
        createPlayerAt(spawnPosition.x, spawnPosition.y);
    }

    private void spawnEnemies(TiledMap map) {
        // Processus de spawn : on lit la couche "SLIMES" et on regroupe les tiles contiguës horizontalement
        // pour définir une zone de patrouille (leftBound/rightBound). Les tiles sont ensuite vidées pour éviter
        // de les re-rendre par le renderer. Les identifiants (enemyId) servent à persister les morts.
        clearExistingEnemies();
        if (map == null) {
            return;
        }

        if (enemyTexture == null) {
            createEnemyTexture();
        }

        TiledMapTileLayer enemyLayer = locateEnemyLayer(map, "SLIMES");
        if (enemyLayer == null) {
            return;
        }

        float tileWidth = enemyLayer.getTileWidth() * Lynguard.UNIT_SCALE;
        float tileHeight = enemyLayer.getTileHeight() * Lynguard.UNIT_SCALE;
        int layerWidth = enemyLayer.getWidth();
        int layerHeight = enemyLayer.getHeight();

        for (int row = 0; row < layerHeight; row++) {
            int col = 0;
            while (col < layerWidth) {
                TiledMapTileLayer.Cell cell = enemyLayer.getCell(col, row);
                if (cell == null || cell.getTile() == null) {
                    col++;
                    continue;
                }

                int startCol = col;
                int endCol = col;
                while (endCol + 1 < layerWidth) {
                    TiledMapTileLayer.Cell nextCell = enemyLayer.getCell(endCol + 1, row);
                    if (nextCell == null || nextCell.getTile() == null) {
                        break;
                    }
                    endCol++;
                }

                float leftBound = startCol * tileWidth;
                float rightBound = (endCol + 1) * tileWidth;
                float spanWidth = rightBound - leftBound;
                float spawnX = leftBound + Math.max(0f, spanWidth - ENEMY_WIDTH) * 0.5f;
                float spawnY = (row + 1) * tileHeight - ENEMY_HEIGHT;

                // Identifiant simple basé sur position arrondie. Suffisant tant qu'on n'a pas besoin de sérialiser
                // plus d'attributs. TODO: Introduire un système UUID quand des variations d'ennemi seront ajoutées.
                String enemyId = (int)spawnX + "_" + (int)spawnY;
                boolean isDead = false;
                if (currentLevel != null) {
                    java.util.Set<String> dead = deadEnemiesPerLevel.get(currentLevel.name());
                    if (dead != null && dead.contains(enemyId)) {
                        isDead = true;
                    }
                }

                if (!isDead) {
                    Entity enemy = createEnemy(leftBound, rightBound, spawnX, spawnY);
                    if (enemy != null) {
                        Enemy enemyComp = Enemy.MAPPER.get(enemy);
                        if (enemyComp != null) enemyComp.id = enemyId;
                        enemyEntities.add(enemy);
                        this.engine.addEntity(enemy);
                    }
                }

                for (int clearCol = startCol; clearCol <= endCol; clearCol++) {
                    enemyLayer.setCell(clearCol, row, null);
                }

                col = endCol + 1;
            }
        }

                if (enemyEntities.isEmpty()) {
            try {
                MapAsset referenceLevel = currentLevel != null ? currentLevel : MapAsset.LEVEL_0;
                com.badlogic.gdx.math.Vector2 playerSpawn = findPlayerSpawn(map, referenceLevel);
                // Align fallback spawn to sit on the top of the tile the player spawn was found in
                float baseY = playerSpawn.y + tileHeight - ENEMY_HEIGHT;
                float left = Math.max(0f, playerSpawn.x - 4f);
                float right = playerSpawn.x + 4f;
                Entity fallbackEnemy = createEnemy(left, right, left, baseY);
                if (fallbackEnemy != null) {
                    enemyEntities.add(fallbackEnemy);
                    this.engine.addEntity(fallbackEnemy);
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private TiledMapTileLayer locateEnemyLayer(TiledMap map, String layerName) {
        if (map == null || layerName == null) {
            return null;
        }

        com.badlogic.gdx.maps.MapLayer direct = map.getLayers().get(layerName);
        if (direct instanceof TiledMapTileLayer tileLayer) {
            return tileLayer;
        }

        for (int i = 0; i < map.getLayers().getCount(); i++) {
            com.badlogic.gdx.maps.MapLayer candidate = map.getLayers().get(i);
            if (candidate instanceof TiledMapTileLayer tileCandidate && layerName.equalsIgnoreCase(candidate.getName())) {
                return tileCandidate;
            }
        }

        return null;
    }

    private void clearExistingEnemies() {
        for (Entity enemy : enemyEntities) {
            this.engine.removeEntity(enemy);
        }
        enemyEntities.clear();
    }

    private Entity createEnemy(float leftBound, float rightBound, float spawnX, float spawnY) {
        // Construit une entité slime : Transform, Graphic, Collider, Patrouille, Animation...
        // La zone de patrouille est déterminée par les bounds calculés à partir des tiles consécutives.
        if (enemyTexture == null) {
            return null;
        }

        float minBound = Math.min(leftBound, rightBound);
        float maxBound = Math.max(leftBound, rightBound);
        float clampedSpawnX = com.badlogic.gdx.math.MathUtils.clamp(spawnX, minBound, maxBound);
        boolean startMovingRight = clampedSpawnX <= minBound + 0.01f;

        Entity enemy = this.engine.createEntity();
        com.badlogic.gdx.math.Vector2 position = new com.badlogic.gdx.math.Vector2(clampedSpawnX, spawnY);
        com.badlogic.gdx.math.Vector2 size = new com.badlogic.gdx.math.Vector2(ENEMY_WIDTH, ENEMY_HEIGHT);
        com.badlogic.gdx.math.Vector2 scaling = new com.badlogic.gdx.math.Vector2(1f, 1f);

    // Contact damage for slimes set to 25 HP (player will lose 25 life on touch)
    Enemy enemyComponent = new Enemy(25f, 0.75f);
        enemy.add(enemyComponent);
        enemy.add(new Transform(position, 50, size, scaling, 0f));

        TextureRegion enemyRegion = null;
        try {
            this.assetService.load(eu.epitech.lil7_games.asset.AtlasAsset.OBJECTS);
            TextureAtlas atlas = this.assetService.get(eu.epitech.lil7_games.asset.AtlasAsset.OBJECTS);
            enemyRegion = atlas.findRegion("ennemi/slime/slime-idle-0");
            if (enemyRegion == null) {
                com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> fallbackRegions = atlas.findRegions("ennemi/slime/slime-idle");
                if (fallbackRegions != null && fallbackRegions.size > 0) {
                    enemyRegion = fallbackRegions.first();
                }
            }
        } catch (Exception ex) {
            Gdx.app.error("GameScreen", "Failed to load slime atlas region: " + ex.getMessage(), ex);
        }

        if (enemyRegion == null) {
            if (enemyTexture == null) {
                createEnemyTexture();
            }
            enemyRegion = new TextureRegion(enemyTexture);
        }

        enemy.add(new Graphic(Color.WHITE.cpy(), enemyRegion));
        float colliderWidth = size.x * ENEMY_COLLIDER_SCALE_X;
        float colliderHeight = size.y * ENEMY_COLLIDER_SCALE_Y;
        float colliderOffsetX = (size.x - colliderWidth) * 0.5f;
        float colliderOffsetY = (size.y - colliderHeight) * 0.5f;
        enemy.add(new Collider(colliderWidth, colliderHeight, colliderOffsetX, colliderOffsetY));
    enemy.add(new Velocity());
    enemy.add(new eu.epitech.lil7_games.component.Life(3, 0f));
        enemy.add(new Patrol(minBound, maxBound, DEFAULT_ENEMY_SPEED, startMovingRight));
        enemy.add(new Facing(startMovingRight ? Facing.FacingDirection.RIGHT : Facing.FacingDirection.LEFT));
        enemy.add(new Flip(!startMovingRight, false));
        enemy.add(new Animation2D(eu.epitech.lil7_games.asset.AtlasAsset.OBJECTS, "ennemi/slime", Animation2D.AnimationType.IDLE, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP, 1f));
        // enemy.add(new Hazard(Hazard.HazardType.ENEMY, enemyComponent.getContactDamage(), true));

        return enemy;
    }

    private void handlePlayerDeath(HazardSystem.HazardContact contact) {
        Gdx.app.log("GameScreen", String.format("Player hit hazard (%s) from %s", contact.getType(), contact.getOrigin()));
        if (playerEntity != null) {
            eu.epitech.lil7_games.component.Life life = eu.epitech.lil7_games.component.Life.MAPPER.get(playerEntity);
            if (life != null && life.getLife() > 0f) {
                life.addLife(-life.getLife());
            }
        }
    }

    private void respawnPlayer() {
        if (!pendingRespawn) {
            return;
        }
        if (currentMap == null || currentLevel == null) {
            pendingRespawn = false;
            return;
        }
        createPlayerAtMapSpawn(currentMap, currentLevel);
    }

    private com.badlogic.gdx.math.Vector2 findPlayerSpawn(com.badlogic.gdx.maps.tiled.TiledMap map, MapAsset mapAsset) {
        String spawnPropertyName = "player_spawn_" + mapAsset.name().toLowerCase();
        com.badlogic.gdx.Gdx.app.log("GameScreen", "Looking for spawn property: " + spawnPropertyName);

        for (com.badlogic.gdx.maps.MapLayer layer : map.getLayers()) {
            if (!(layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer)) {
                continue;
            }

            if ("PLAYER_SPAWN".equals(layer.getName())) {
                com.badlogic.gdx.Gdx.app.log("GameScreen", "Found PLAYER_SPAWN layer");
                com.badlogic.gdx.maps.MapProperties layerProps = layer.getProperties();

                if (layerProps.containsKey(spawnPropertyName)) {
                    com.badlogic.gdx.Gdx.app.log("GameScreen", "Property match found!");
                    com.badlogic.gdx.maps.tiled.TiledMapTileLayer tileLayer = (com.badlogic.gdx.maps.tiled.TiledMapTileLayer) layer;
                    com.badlogic.gdx.math.Vector2 spawn = findFirstFilledCellWorldPos(tileLayer);
                    if (spawn != null) {
                        com.badlogic.gdx.Gdx.app.log("GameScreen", String.format("Found spawn in layer '%s' at (%.2f, %.2f)", layer.getName(), spawn.x, spawn.y));
                        return spawn;
                    }
                }
            }
        }

        throw new RuntimeException("No PLAYER_SPAWN found for " + mapAsset.name() + " with property " + spawnPropertyName);
    }

    private com.badlogic.gdx.math.Vector2 findReturnSpawn(com.badlogic.gdx.maps.tiled.TiledMap map, String returnProperty) {
        if (returnProperty == null || returnProperty.isBlank()) {
            com.badlogic.gdx.Gdx.app.log("GameScreen", "No return property specified, will use default spawn");
            return null;
        }

        com.badlogic.gdx.Gdx.app.log("GameScreen", "Searching for return spawn with property: " + returnProperty);

        for (com.badlogic.gdx.maps.MapLayer layer : map.getLayers()) {
            if (!(layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer tileLayer)) {
                continue;
            }

            if (!layer.getProperties().containsKey(returnProperty)) {
                continue;
            }

            com.badlogic.gdx.Gdx.app.log("GameScreen", "Found layer '" + layer.getName() + "' with property '" + returnProperty + "'");

            com.badlogic.gdx.math.Vector2 spawn = findFirstFilledCellWorldPos(tileLayer);
            if (spawn != null) {
                com.badlogic.gdx.Gdx.app.log("GameScreen", String.format("Return spawn '%s' located on layer '%s' at (%.2f, %.2f)",
                    returnProperty, layer.getName(), spawn.x, spawn.y));
                return spawn;
            }
        }

        com.badlogic.gdx.Gdx.app.error("GameScreen", "Unable to find return spawn property '" + returnProperty + "' on map");
        return null;
    }

    private com.badlogic.gdx.math.Vector2 findFirstFilledCellWorldPos(com.badlogic.gdx.maps.tiled.TiledMapTileLayer tileLayer) {
        for (int x = 0; x < tileLayer.getWidth(); x++) {
            for (int y = 0; y < tileLayer.getHeight(); y++) {
                com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    float worldX = x * tileLayer.getTileWidth() * Lynguard.UNIT_SCALE;
                    float worldY = y * tileLayer.getTileHeight() * Lynguard.UNIT_SCALE;
                    return new com.badlogic.gdx.math.Vector2(worldX, worldY);
                }
            }
        }
        return null;
    }

    private void createPlayerAt(float x, float y) {
        // Respawn / création : on préserve les clés collectées avant suppression.
        // Le collider tente d'être ajusté via PixelTrimUtil pour coller au sprite réel (moins de faux positifs).
        if (playerEntity != null) {
            Player oldPlayer = Player.MAPPER.get(playerEntity);
            if (oldPlayer != null) {
                this.persistentKeys = new java.util.ArrayList<>(oldPlayer.keys);
            }
            this.engine.removeEntity(playerEntity);
        }

        if (playerTexture == null) {
            createPlayerTexture();
        }

    playerEntity = this.engine.createEntity();
    com.badlogic.gdx.math.Vector2 position = new com.badlogic.gdx.math.Vector2(x, y);
	com.badlogic.gdx.math.Vector2 size = new com.badlogic.gdx.math.Vector2(1.6f, 1.6f);
	com.badlogic.gdx.math.Vector2 scaling = new com.badlogic.gdx.math.Vector2(1f, 1f);

    Player playerComp = new Player();
    playerComp.keys.addAll(this.persistentKeys);
    playerEntity.add(playerComp);
    
    hudViewModel.setKeys(this.persistentKeys);

    playerEntity.add(new Dash());
    playerEntity.add(new Life(100, 0));
    playerEntity.add(new eu.epitech.lil7_games.component.Attack());
    playerEntity.add(new eu.epitech.lil7_games.component.Defense());
    playerEntity.add(new Transform(position, 100, size, scaling, 0f));

    this.assetService.load(eu.epitech.lil7_games.asset.AtlasAsset.OBJECTS);
    playerEntity.add(new Graphic(Color.WHITE, new TextureRegion(playerTexture)));
    playerEntity.add(new Facing(Facing.FacingDirection.RIGHT));
    playerEntity.add(new Flip());
    playerEntity.add(new Animation2D(eu.epitech.lil7_games.asset.AtlasAsset.OBJECTS, "player", Animation2D.AnimationType.IDLE, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP, 1f));
    float colliderW = size.x * scaling.x;
    float colliderH = size.y * scaling.y;
    float colliderOffsetY = 0f;
    try {
        com.badlogic.gdx.graphics.g2d.TextureAtlas atlas = this.assetService.get(eu.epitech.lil7_games.asset.AtlasAsset.OBJECTS);
        com.badlogic.gdx.graphics.g2d.TextureRegion sampleRegion = null;
        for (com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion r : atlas.getRegions()) {
            String sanitized = r.name.replace(" ", "");
            if (sanitized.startsWith("player/Idle")) {
                sampleRegion = r;
                break;
            }
        }
        if (sampleRegion != null) {
            eu.epitech.lil7_games.util.PixelTrimUtil.PaddingResult padding = eu.epitech.lil7_games.util.PixelTrimUtil.computePadding(sampleRegion, 5);
            if (padding.regionPixelHeight > 0 && padding.regionPixelWidth > 0) {
                float bottomFrac = padding.bottomTrim / (float) padding.regionPixelHeight;
                float leftFrac = padding.leftTrim / (float) padding.regionPixelWidth;
                float rightFrac = padding.rightTrim / (float) padding.regionPixelWidth;
                if (bottomFrac > 0f && bottomFrac < 0.9f) {
                    colliderOffsetY = size.y * scaling.y * bottomFrac;
                    colliderH = size.y * scaling.y * (1f - bottomFrac);
                }
                if ((leftFrac + rightFrac) > 0f && (leftFrac + rightFrac) < 0.9f) {
                    float newWidth = size.x * scaling.x * (1f - leftFrac - rightFrac);
                    float offsetX = size.x * scaling.x * leftFrac;
                    colliderW = newWidth;
                    playerEntity.add(new Collider(colliderW, colliderH, offsetX, colliderOffsetY));
                    Gdx.app.log("PixelTrim", String.format("Applied trim B:%dpx L:%dpx R:%dpx => offsets (x=%.3f,y=%.3f) size(%.3f,%.3f)", padding.bottomTrim, padding.leftTrim, padding.rightTrim, offsetX, colliderOffsetY, colliderW, colliderH));
                } else {
                    playerEntity.add(new Collider(colliderW, colliderH, 0f, colliderOffsetY));
                    Gdx.app.log("PixelTrim", String.format("Applied bottom trim %dpx; no horizontal trim.", padding.bottomTrim));
                }
            } else {
                playerEntity.add(new Collider(colliderW, colliderH, 0f, colliderOffsetY));
                Gdx.app.log("PixelTrim", "Padding result invalid dimensions; skipping.");
            }
        } else {
            playerEntity.add(new Collider(colliderW, colliderH, 0f, colliderOffsetY));
            Gdx.app.log("PixelTrim", "No idle frame found for player; skipping trimming.");
        }
    } catch (Exception ex) {
        playerEntity.add(new Collider(colliderW, colliderH, 0f, colliderOffsetY));
        Gdx.app.error("PixelTrim", "Failed trimming collider: " + ex.getMessage(), ex);
    }
        playerEntity.add(new Velocity());
        playerEntity.add(new Physics());
        this.engine.addEntity(playerEntity);
        this.hazardSystem.onPlayerRespawn();
        this.pendingRespawn = false;
    }

    public void setSaveData(SaveData data) {
        this.saveDataToLoad = data;
    }

    public java.util.Map<String, java.util.Set<String>> getDeadEnemiesPerLevel() {
        return new java.util.HashMap<>(deadEnemiesPerLevel);
    }

    public MapAsset getCurrentLevel() {
        return currentLevel;
    }

    public SaveData createSaveData(String slotName) {
        SaveData data = new SaveData();
        data.slotName = slotName;
        data.timestamp = com.badlogic.gdx.utils.TimeUtils.millis();
        
        if (currentLevel != null) {
            data.levelName = currentLevel.name();
        }
        
        if (playerEntity != null) {
            Transform transform = Transform.MAPPER.get(playerEntity);
            if (transform != null) {
                data.playerX = transform.getPosition().x;
                data.playerY = transform.getPosition().y;
            }
            
            Life life = Life.MAPPER.get(playerEntity);
            if (life != null) {
                data.playerLife = life.getLife();
                data.playerMaxLife = life.getMaxLife();
            }
            
            Player player = Player.MAPPER.get(playerEntity);
            if (player != null) {
                data.playerKeys = new java.util.ArrayList<>(player.keys);
            }
        }
        
        for (java.util.Map.Entry<String, java.util.Set<String>> entry : deadEnemiesPerLevel.entrySet()) {
            data.deadEnemies.put(entry.getKey(), new java.util.ArrayList<>(entry.getValue()));
        }
        
        return data;
    }

    private void loadSaveData(SaveData data) {
        if (initialized) {
            engine.removeAllEntities();
            persistentKeys.clear();
            deadEnemiesPerLevel.clear();
        }
        
        if (data.deadEnemies != null) {
            for (java.util.Map.Entry<String, java.util.List<String>> entry : data.deadEnemies.entrySet()) {
                deadEnemiesPerLevel.put(entry.getKey(), new java.util.HashSet<>(entry.getValue()));
            }
        }

        MapAsset level = MapAsset.LEVEL_0;
        if (data.levelName != null) {
            try {
                level = MapAsset.valueOf(data.levelName);
            } catch (IllegalArgumentException e) {
                Gdx.app.error("GameScreen", "Unknown level in save data: " + data.levelName);
            }
        }
        
        TiledMap tiledMap = this.tiledService.loadMap(level);
        this.tiledService.setMap(tiledMap);
        this.currentMap = tiledMap;
        this.currentLevel = level;
        
        spawnEnemies(tiledMap);
        
        createPlayerAt(data.playerX, data.playerY);
        
        if (playerEntity != null) {
            Life life = Life.MAPPER.get(playerEntity);
            if (life != null) {
                life.setMaxLife(data.playerMaxLife);
                life.setLife(data.playerLife);
            }
            
            Player player = Player.MAPPER.get(playerEntity);
            if (player != null) {
                player.keys.clear();
                player.keys.addAll(data.playerKeys);
            }
        }
    }

    private void onLevelTransition(LevelTransitionEvent event) {
        // Transition de niveau : on purge toutes les entités sauf le joueur (qui est recréé).
        // Pour un retour (RETURN), on tente un spawn dédié; sinon spawn classique.
        MapAsset targetLevel = event.getTargetLevel();
        com.badlogic.gdx.Gdx.app.log("GameScreen", String.format("Level transition triggered: %s (%s)", targetLevel.name(), event.getTransitionType()));
        com.badlogic.ashley.utils.ImmutableArray<Entity> entities = this.engine.getEntities();
        com.badlogic.gdx.utils.Array<Entity> entitiesToRemove = new com.badlogic.gdx.utils.Array<>();

        for (Entity entity : entities) {
            if (entity != playerEntity) {
                entitiesToRemove.add(entity);
            }
        }

        for (Entity entity : entitiesToRemove) {
            this.engine.removeEntity(entity);
        }
        enemyEntities.clear();

        com.badlogic.gdx.Gdx.app.log("GameScreen", "Removed " + entitiesToRemove.size + " entities from previous level");

        TiledMap newMap = this.tiledService.loadMap(targetLevel);
        this.tiledService.setMap(newMap);
        this.currentMap = newMap;
        this.currentLevel = targetLevel;

        com.badlogic.gdx.math.Vector2 returnSpawn = null;
        if (event.getTransitionType() == TransitionType.RETURN) {
            returnSpawn = findReturnSpawn(newMap, event.getReturnSpawnProperty());
        }

        if (returnSpawn != null) {
            com.badlogic.gdx.Gdx.app.log("GameScreen", String.format("Using return spawn at (%.2f, %.2f)", returnSpawn.x, returnSpawn.y));
            createPlayerAt(returnSpawn.x, returnSpawn.y);
        } else {
            createPlayerAtMapSpawn(newMap, targetLevel);
        }

        spawnEnemies(newMap);

        com.badlogic.gdx.Gdx.app.log("GameScreen", "Level transition completed");
        
        game.getMenuViewModel().autoSave();
    }

    @Override
    public void hide(){
        if (!game.isGameRunning()) {
            clearExistingEnemies();
            this.engine.removeAllEntities();
            this.stage.clear();
            this.initialized = false;
        }
        this.pendingRespawn = false;
    }

    @Override
    public void render(float delta){
        delta = Math.min(delta, 1/30f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){
            game.setScreen(PauseScreen.class);
            return;
        }
        
        checkPlayerDeath();
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) && playerEntity != null) {
            eu.epitech.lil7_games.component.NoGravityCooldown cooldown = eu.epitech.lil7_games.component.NoGravityCooldown.MAPPER.get(playerEntity);
            if (NoGravity.MAPPER.get(playerEntity) == null) {
                if (cooldown == null || cooldown.getRemainingSeconds() <= 0f) {
                    playerEntity.add(new NoGravity());
                    playerEntity.add(new eu.epitech.lil7_games.component.NoGravityCooldown(1.5f));
                } else {
                }
            } else {
                playerEntity.remove(NoGravity.class);
            }
        }
    this.engine.update(delta);
    
    // Auto-save toutes les 60 secondes (simple timer incrémental).
    autoSaveTimer += delta;
    if (autoSaveTimer >= 60f) {
        game.getMenuViewModel().autoSave();
        autoSaveTimer = 0f;
    }
    
    updateHud();

        if (playerEntity != null && this.audioService != null) {
            Transform t = Transform.MAPPER.get(playerEntity);
            if (t != null) {
                this.audioService.updateListenerPosition(t.getPosition().x, t.getPosition().y);
            }
        }

        if (pendingRespawn) {
            respawnPlayer();
        }

        uiViewport.apply();
        stage.getBatch().setColor(Color.WHITE);
        stage.act(delta);
        stage.draw();
    }

    private void updateHud() {
        if (playerEntity == null) return;

        Life life = Life.MAPPER.get(playerEntity);
        if (life != null) {
            hudViewModel.setLife(life.getLife());
            hudViewModel.setMaxLife(life.getMaxLife());
        }

        Dash dash = Dash.MAPPER.get(playerEntity);
        if (dash != null) {
            hudViewModel.setDashCooldown(dash.getCooldownRemaining());
            hudViewModel.setMaxDashCooldown(1.0f);
            hudViewModel.setDashReady(dash.getCooldownRemaining() <= 0);
        }
        
        boolean currentlyPossible = hudViewModel.isActionPossible();
        hudViewModel.setActionPossible(currentlyPossible || playerSystem.canDropThrough(playerEntity));
    }

    private void checkPlayerDeath() {
        if (playerEntity == null) return;
        Life life = Life.MAPPER.get(playerEntity);
        if (life != null && life.getLife() <= 0) {
            Dying dying = Dying.MAPPER.get(playerEntity);
            if (dying == null) {
                eu.epitech.lil7_games.component.Animation2D animation2D = eu.epitech.lil7_games.component.Animation2D.MAPPER.get(playerEntity);
                if (animation2D != null) {
                    animation2D.setType(eu.epitech.lil7_games.component.Animation2D.AnimationType.DEATH);
                    animation2D.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
                }

                Velocity velocity = Velocity.MAPPER.get(playerEntity);
                if (velocity != null) velocity.getVelocity().setZero();
                float dyingDuration = -1f;
                if (animation2D != null) {
                    eu.epitech.lil7_games.component.Facing.FacingDirection dir = eu.epitech.lil7_games.component.Facing.MAPPER.get(playerEntity) != null ? eu.epitech.lil7_games.component.Facing.MAPPER.get(playerEntity).getDirection() : eu.epitech.lil7_games.component.Facing.FacingDirection.RIGHT;
                    com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim = this.animationSystem.ensureAnimation(animation2D, dir);
                    if (anim != null) {
                        float animDuration = anim.getAnimationDuration();
                        float speed = animation2D.getSpeed();
                        if (speed <= 0f) speed = 1f;
                        dyingDuration = animDuration / speed;
                    }
                }

                if (dyingDuration > 0f) {
                    playerEntity.add(new Dying(dyingDuration + 0.1f));
                } else {
                    playerEntity.add(new Dying());
                }
                return;
            }

            if (playerEntity != null && !this.engine.getEntities().contains(playerEntity, true)) {
                playerEntity = null;
                game.setScreen(DeathScreen.class);
            }
        }
    }

    @Override
    public void dispose(){
        for (EntitySystem system : this.engine.getSystems()) {
            if (system instanceof Disposable disposableSystem){
                disposableSystem.dispose();
            }
        }

        if (playerTexture != null) {
            playerTexture.dispose();
        }
        if (enemyTexture != null) {
            enemyTexture.dispose();
        }
        this.stage.dispose();
    }
}
