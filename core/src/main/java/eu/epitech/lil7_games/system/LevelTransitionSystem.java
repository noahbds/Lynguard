package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.MapAsset;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class LevelTransitionSystem extends IteratingSystem {
    private TiledMap currentMap;
    private MapAsset currentMapAsset;
    private LevelTransitionListener transitionListener;
    private boolean hasTriggeredTransition = false;

    private static final String LEVEL_TRANSITION_PREFIX = "level_transition_";
    private static final String RETURN_TRANSITION_PREFIX = "player_return_";
    private static final String RETURN_TARGET_PREFIX = "return_from_";

    public interface LevelTransitionListener {
        void onTransition(LevelTransitionEvent event);
    }

    public enum TransitionType {
        FORWARD,
        RETURN
    }

    public static final class LevelTransitionEvent {
        private final MapAsset targetLevel;
        private final TransitionType transitionType;
        private final String returnSpawnProperty;
        private final MapAsset sourceLevel;

        public LevelTransitionEvent(MapAsset targetLevel, TransitionType transitionType, String returnSpawnProperty, MapAsset sourceLevel) {
            this.targetLevel = targetLevel;
            this.transitionType = transitionType;
            this.returnSpawnProperty = returnSpawnProperty;
            this.sourceLevel = sourceLevel;
        }

        public MapAsset getTargetLevel() {
            return targetLevel;
        }

        public TransitionType getTransitionType() {
            return transitionType;
        }

        public String getReturnSpawnProperty() {
            return returnSpawnProperty;
        }

        public MapAsset getSourceLevel() {
            return sourceLevel;
        }
    }

    public LevelTransitionSystem() {
        super(Family.all(Player.class, Transform.class).get());
    }

    // Track transitions (property keys) that have been unlocked for each map so state survives map reloads
    private final Map<MapAsset, Set<String>> unlockedTransitions = new HashMap<>();

    public void setMap(TiledMap map) {
        this.currentMap = map;
        this.hasTriggeredTransition = false;
        if (map != null) {
            Gdx.app.log("LevelTransitionSystem", "Map set. Layers:");
            for (MapLayer layer : map.getLayers()) {
                Gdx.app.log("LevelTransitionSystem", "  Layer: " + layer.getName());
                java.util.Iterator<String> keys = layer.getProperties().getKeys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Gdx.app.log("LevelTransitionSystem", "    Property: " + key + " = " + layer.getProperties().get(key));
                }
            }
            this.currentMapAsset = map.getProperties().get("mapAsset", MapAsset.class);
            if (this.currentMapAsset == null) {
                Gdx.app.error("LevelTransitionSystem", "Current map is missing mapAsset property; return transitions may fail.");
            }
        } else {
            this.currentMapAsset = null;
        }
        Gdx.app.log("LevelTransitionSystem", "Map changed, reset transition flag");
        // If this map has transitions that were unlocked earlier, apply those changes now so doors stay unlocked
        if (this.currentMapAsset != null && unlockedTransitions.containsKey(this.currentMapAsset)) {
            Set<String> unlocked = unlockedTransitions.get(this.currentMapAsset);
            if (unlocked != null && !unlocked.isEmpty()) {
                Gdx.app.log("LevelTransitionSystem", "Applying " + unlocked.size() + " unlocked transitions to map " + this.currentMapAsset.name());
                applyUnlockedToMap(map, unlocked);
            }
        }
    }

    public void markTransitionUnlocked(String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank() || this.currentMapAsset == null) return;
        Set<String> set = unlockedTransitions.computeIfAbsent(this.currentMapAsset, k -> new HashSet<>());
        set.add(propertyKey);
        Gdx.app.log("LevelTransitionSystem", "Marked transition unlocked for map " + this.currentMapAsset + " -> " + propertyKey);
    }

    private void applyUnlockedToMap(TiledMap map, Set<String> properties) {
        if (map == null || properties == null || properties.isEmpty()) return;
        for (MapLayer layer : map.getLayers()) {
            MapProperties layerProps = layer.getProperties();
            java.util.Iterator<String> keys = layerProps.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (properties.contains(key)) {
                    // ensure door not closed
                    try { layerProps.put("isclosed", "FALSE"); } catch (Exception ex) { /* ignore */ }
                }
            }

            // Also handle objects on object layers
            if (layer.getObjects() != null && layer.getObjects().getCount() > 0) {
                for (com.badlogic.gdx.maps.MapObject object : layer.getObjects()) {
                    MapProperties objProps = object.getProperties();
                    java.util.Iterator<String> objKeys = objProps.getKeys();
                    while (objKeys.hasNext()) {
                        String k = objKeys.next();
                        if (properties.contains(k)) {
                            try { objProps.put("isclosed", "FALSE"); } catch (Exception ex) { /* ignore */ }
                        }
                    }
                }
            }
        }
    }

    public void setTransitionListener(LevelTransitionListener listener) {
        this.transitionListener = listener;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (currentMap == null || transitionListener == null || hasTriggeredTransition) {
            return;
        }

        Transform transform = Transform.MAPPER.get(entity);
        Vector2 position = transform.getPosition();
        Vector2 size = transform.getSize();

        float playerMinX = position.x;
        float playerMinY = position.y;
        Rectangle playerBounds = new Rectangle(playerMinX, playerMinY, size.x, size.y);

        if (evaluateTransitionLayers(playerBounds, false)) {
            return;
        }
        evaluateTransitionLayers(playerBounds, true);
    }

    private boolean evaluateTransitionLayers(Rectangle playerBounds, boolean returnLayersOnly) {
        for (MapLayer layer : currentMap.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer tileLayer)) {
                continue;
            }

            boolean layerIsReturn = isReturnLayer(layer.getName());
            if (returnLayersOnly && !layerIsReturn) {
                continue;
            }
            if (!returnLayersOnly && layerIsReturn) {
                continue;
            }

            MapProperties layerProps = layer.getProperties();
            
            if (layerProps.containsKey("isclosed") && "TRUE".equals(layerProps.get("isclosed", String.class))) {
                continue;
            }

            java.util.Iterator<String> keys = layerProps.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!returnLayersOnly && key.startsWith(LEVEL_TRANSITION_PREFIX)) {
                    handleForwardTransition(tileLayer, key, playerBounds);
                } else if (returnLayersOnly && key.startsWith(RETURN_TRANSITION_PREFIX)) {
                    handleReturnTransition(tileLayer, key, playerBounds);
                } else if (key.startsWith(RETURN_TARGET_PREFIX)) {
                    // Spawn marker used only when resolving return destinations; no action needed.
                }

                if (hasTriggeredTransition) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleForwardTransition(TiledMapTileLayer tileLayer, String propertyKey, Rectangle playerBounds) {
        MapAsset targetLevel = parseTargetLevel(propertyKey, LEVEL_TRANSITION_PREFIX);
        if (targetLevel == null) {
            return;
        }

        // Hack: Prevent phantom transition to LEVEL_8 from LEVEL_6
        if (currentMapAsset == MapAsset.LEVEL_6 && targetLevel == MapAsset.LEVEL_8) {
            Gdx.app.log("LevelTransitionSystem", "Ignoring phantom transition to LEVEL_8 from LEVEL_6 on layer: " + tileLayer.getName());
            return;
        }

        if (!isPlayerOnActiveCell(tileLayer, playerBounds)) {
            return;
        }

        Gdx.app.log("LevelTransitionSystem", "Triggering forward transition to " + targetLevel.name() + " from layer " + tileLayer.getName());
        triggerTransition(targetLevel, TransitionType.FORWARD, null);
    }

    private void handleReturnTransition(TiledMapTileLayer tileLayer, String propertyKey, Rectangle playerBounds) {
        MapAsset targetLevel = parseTargetLevel(propertyKey, RETURN_TRANSITION_PREFIX);
        if (targetLevel == null) {
            return;
        }

        if (!isPlayerOnActiveCell(tileLayer, playerBounds)) {
            return;
        }

        String returnProperty = buildReturnPropertyKey();
        Gdx.app.log("LevelTransitionSystem", "Triggering return transition to " + targetLevel.name() + " using property " + returnProperty);
        triggerTransition(targetLevel, TransitionType.RETURN, returnProperty);
    }

    private MapAsset parseTargetLevel(String propertyKey, String prefix) {
        String targetLevelRaw = propertyKey.substring(prefix.length());
        if (targetLevelRaw.isEmpty()) {
            Gdx.app.error("LevelTransitionSystem", "Transition property missing target level suffix: " + propertyKey);
            return null;
        }

        String normalized = targetLevelRaw.replaceAll("(?<=[A-Za-z])(?=\\d)", "_").toUpperCase(Locale.ROOT);
        try {
            return MapAsset.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            Gdx.app.error("LevelTransitionSystem", "Invalid target level name: " + normalized, e);
            return null;
        }
    }

    private boolean isPlayerOnActiveCell(TiledMapTileLayer tileLayer, Rectangle playerBounds) {
        float tileWidth = tileLayer.getTileWidth() * Lynguard.UNIT_SCALE;
        float tileHeight = tileLayer.getTileHeight() * Lynguard.UNIT_SCALE;
        int leftTile = Math.max(0, (int) (playerBounds.x / tileWidth));
        int rightTile = Math.min(tileLayer.getWidth() - 1, (int) ((playerBounds.x + playerBounds.width) / tileWidth));
        int bottomTile = Math.max(0, (int) (playerBounds.y / tileHeight));
        int topTile = Math.min(tileLayer.getHeight() - 1, (int) ((playerBounds.y + playerBounds.height) / tileHeight));

        Rectangle cellBounds = new Rectangle(0f, 0f, tileWidth, tileHeight);
        for (int y = bottomTile; y <= topTile; y++) {
            for (int x = leftTile; x <= rightTile; x++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell == null || cell.getTile() == null) {
                    continue;
                }

                cellBounds.setPosition(x * tileWidth, y * tileHeight);
                if (playerBounds.overlaps(cellBounds)) {
                    return true;
                }
            }
        }

        Gdx.app.log("LevelTransitionSystem", "No active return/transition tile under player in layer " + tileLayer.getName());
        return false;
    }

    private void triggerTransition(MapAsset targetLevel, TransitionType type, String returnSpawnProperty) {
        if (transitionListener == null || targetLevel == null) {
            return;
        }
        hasTriggeredTransition = true;
        transitionListener.onTransition(new LevelTransitionEvent(targetLevel, type, returnSpawnProperty, currentMapAsset));
    }

    private boolean isReturnLayer(String layerName) {
        if (layerName == null) {
            return false;
        }
        String normalized = layerName.toUpperCase(Locale.ROOT);
        boolean isReturn = normalized.startsWith("RETURN_LEVEL") || normalized.startsWith("LEVEL_RETURN");
        if (isReturn) {
            Gdx.app.log("LevelTransitionSystem", "Detected return layer: " + layerName);
        }
        return isReturn;
    }

    private String buildReturnPropertyKey() {
        if (currentMapAsset == null) {
            Gdx.app.error("LevelTransitionSystem", "Cannot build return property without current map asset");
            return null;
        }
        return RETURN_TARGET_PREFIX + currentMapAsset.name().toLowerCase(Locale.ROOT);
    }

    public void triggerTransitionFromProperty(String propertyKey) {
        MapAsset targetLevel = parseTargetLevel(propertyKey, LEVEL_TRANSITION_PREFIX);
        if (targetLevel != null) {
            triggerTransition(targetLevel, TransitionType.FORWARD, null);
        }
    }
}
