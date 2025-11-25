package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.component.Collider;
import eu.epitech.lil7_games.component.Hazard;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;

import java.util.Locale;

public class HazardSystem extends IteratingSystem {
    public interface PlayerDeathListener {
        void onPlayerDeath(HazardContact contact);
    }

    public enum HazardOrigin {
        TILE_LAYER,
        ENTITY
    }

    public static final class HazardContact {
        private final Hazard.HazardType type;
        private final HazardOrigin origin;
        private final String sourceId;
        private final Vector2 position;

        private HazardContact(Hazard.HazardType type, HazardOrigin origin, String sourceId, Vector2 position) {
            this.type = type;
            this.origin = origin;
            this.sourceId = sourceId;
            this.position = position;
        }

        public static HazardContact tileContact(String layerName, Hazard.HazardType type, float x, float y) {
            return new HazardContact(type, HazardOrigin.TILE_LAYER, layerName, new Vector2(x, y));
        }

        public static HazardContact entityContact(String entityId, Hazard hazard, Vector2 position) {
            return new HazardContact(hazard.getType(), HazardOrigin.ENTITY, entityId, position.cpy());
        }

        public Hazard.HazardType getType() {
            return type;
        }

        public HazardOrigin getOrigin() {
            return origin;
        }

        public String getSourceId() {
            return sourceId;
        }

        public Vector2 getPosition() {
            return position;
        }
    }

    private static final Family HAZARD_FAMILY = Family.all(Hazard.class, Transform.class).exclude(Player.class).get();
    private static final String HAZARD_PROPERTY_KEY = "hazard";

    private final Rectangle playerBounds;
    private final Rectangle hazardBounds;
    private final Array<LayerHazard> layerHazards;
    private final Vector2 tmpVec;
    private ImmutableArray<Entity> hazardEntities;
    private PlayerDeathListener deathListener;
    private boolean waitingForResolution;
    private TiledMap currentMap;

    private record LayerHazard(TiledMapTileLayer layer,
                              Hazard.HazardType type,
                              String identifier,
                              float marginX,
                              float marginY) {
    }

    public HazardSystem() {
        super(Family.all(Player.class, Transform.class).get());
        this.playerBounds = new Rectangle();
        this.hazardBounds = new Rectangle();
        this.layerHazards = new Array<>();
        this.tmpVec = new Vector2();
        this.waitingForResolution = false;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.hazardEntities = engine.getEntitiesFor(HAZARD_FAMILY);
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
        this.hazardEntities = null;
    }

    public void setMap(TiledMap map) {
        this.currentMap = map;
        rebuildHazardLayers();
        waitingForResolution = false;
    }

    public void setDeathListener(PlayerDeathListener listener) {
        this.deathListener = listener;
    }

    public void onPlayerRespawn() {
        waitingForResolution = false;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (waitingForResolution) {
            return;
        }

        Transform transform = Transform.MAPPER.get(entity);
        Vector2 position = transform.getPosition();
        Collider collider = Collider.MAPPER.get(entity);
        if (collider != null) {
            playerBounds.set(position.x + collider.getOffsetX(),
                position.y + collider.getOffsetY(),
                collider.getWidth(),
                collider.getHeight());
        } else {
            Vector2 size = transform.getSize();
            playerBounds.set(position.x, position.y, size.x, size.y);
        }

        if (checkTileHazards()) {
            return;
        }

        checkEntityHazards();
    }

    private boolean checkTileHazards() {
        if (layerHazards.isEmpty()) {
            return false;
        }

        for (LayerHazard layerHazard : layerHazards) {
            TiledMapTileLayer layer = layerHazard.layer();
            float tileWidth = layer.getTileWidth() * Lynguard.UNIT_SCALE;
            float tileHeight = layer.getTileHeight() * Lynguard.UNIT_SCALE;

            int minCol = MathUtils.clamp((int) Math.floor(playerBounds.x / tileWidth), 0, layer.getWidth() - 1);
            int maxCol = MathUtils.clamp((int) Math.floor((playerBounds.x + playerBounds.width) / tileWidth), 0, layer.getWidth() - 1);
            int minRow = MathUtils.clamp((int) Math.floor(playerBounds.y / tileHeight), 0, layer.getHeight() - 1);
            int maxRow = MathUtils.clamp((int) Math.floor((playerBounds.y + playerBounds.height) / tileHeight), 0, layer.getHeight() - 1);

            if (minCol > maxCol || minRow > maxRow) {
                continue;
            }

            for (int row = minRow; row <= maxRow; row++) {
                for (int col = minCol; col <= maxCol; col++) {
                    TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                    if (cell == null || cell.getTile() == null) {
                        continue;
                    }

                    float tileX = col * tileWidth;
                    float tileY = row * tileHeight;
                    float marginX = layerHazard.marginX() * tileWidth;
                    float marginY = layerHazard.marginY() * tileHeight;
                    float adjustedWidth = Math.max(0f, tileWidth - marginX * 2f);
                    float adjustedHeight = Math.max(0f, tileHeight - marginY * 2f);
                    float adjustedX = tileX + marginX;
                    float adjustedY = tileY + marginY;
                    hazardBounds.set(adjustedX, adjustedY, adjustedWidth, adjustedHeight);

                    if (playerBounds.overlaps(hazardBounds)) {
                        triggerDeath(HazardContact.tileContact(layerHazard.identifier(), layerHazard.type(),
                            tileX + tileWidth * 0.5f, tileY + tileHeight * 0.5f));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void checkEntityHazards() {
        if (hazardEntities == null || hazardEntities.size() == 0) {
            return;
        }

        for (Entity hazardEntity : hazardEntities) {
            Transform hazardTransform = Transform.MAPPER.get(hazardEntity);
            Hazard hazard = Hazard.MAPPER.get(hazardEntity);
            if (hazardTransform == null || hazard == null || !hazard.isActive() || !hazard.isLethal()) {
                continue;
            }

            Vector2 hazardPos = hazardTransform.getPosition();
            Vector2 hazardSize = hazardTransform.getSize();
            hazardBounds.set(hazardPos.x, hazardPos.y, hazardSize.x, hazardSize.y);

            if (playerBounds.overlaps(hazardBounds)) {
                tmpVec.set(playerBounds.x + playerBounds.width * 0.5f, playerBounds.y + playerBounds.height * 0.5f);
                triggerDeath(HazardContact.entityContact(hazardEntity.toString(), hazard, tmpVec));
                return;
            }
        }
    }

    private void triggerDeath(HazardContact contact) {
        if (waitingForResolution) {
            return;
        }
        waitingForResolution = true;
        if (deathListener != null) {
            deathListener.onPlayerDeath(contact);
        }
    }

    private void rebuildHazardLayers() {
        layerHazards.clear();
        if (currentMap == null) {
            return;
        }

        for (MapLayer layer : currentMap.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer tileLayer)) {
                continue;
            }

            Hazard.HazardType type = determineHazardType(layer);
            if (type == null) {
                continue;
            }

            float marginX = readMargin(layer, "hazardMarginX", readMargin(layer, "hazardMargin", 0f));
            float marginY = readMargin(layer, "hazardMarginY", readMargin(layer, "hazardMargin", 0f));
            layerHazards.add(new LayerHazard(tileLayer, type, layer.getName(), marginX, marginY));
        }
    }

    private float readMargin(MapLayer layer, String key, float defaultValue) {
        Object raw = layer.getProperties().get(key);
        if (raw instanceof Number number) {
            return MathUtils.clamp(number.floatValue(), 0f, 0.49f);
        }
        if (raw instanceof String str) {
            try {
                float value = Float.parseFloat(str);
                return MathUtils.clamp(value, 0f, 0.49f);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Hazard.HazardType determineHazardType(MapLayer layer) {
        Object hazardFlag = layer.getProperties().get(HAZARD_PROPERTY_KEY);
        if (hazardFlag instanceof Boolean booleanValue && booleanValue) {
            return hazardTypeFromName(layer.getName());
        }
        if (hazardFlag instanceof String stringValue && Boolean.parseBoolean(stringValue)) {
            return hazardTypeFromName(layer.getName());
        }

        String layerName = layer.getName();
        if (layerName == null) {
            return null;
        }

        String lowerName = layerName.toLowerCase(Locale.ROOT);
        if (lowerName.contains("spike")) {
            return Hazard.HazardType.SPIKES;
        }
        if (lowerName.contains("hazard") || lowerName.contains("lava") || lowerName.contains("acid")) {
            return Hazard.HazardType.ENVIRONMENT;
        }
        return null;
    }

    private Hazard.HazardType hazardTypeFromName(String name) {
        if (name == null) {
            return Hazard.HazardType.ENVIRONMENT;
        }
        return determineHazardTypeName(name);
    }

    private Hazard.HazardType determineHazardTypeName(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (lowerName.contains("spike")) {
            return Hazard.HazardType.SPIKES;
        }
        if (lowerName.contains("enemy")) {
            return Hazard.HazardType.ENEMY;
        }
        return Hazard.HazardType.ENVIRONMENT;
    }
}
