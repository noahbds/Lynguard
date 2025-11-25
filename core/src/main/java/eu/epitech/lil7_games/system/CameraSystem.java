package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;

public class CameraSystem extends IteratingSystem {
    private final OrthographicCamera camera;
    private static final float LERP_SPEED = 5.0f;
    private TiledMap currentMap;
    private float mapWidth;
    private float mapHeight;

    public CameraSystem(OrthographicCamera camera) {
        super(Family.all(Player.class, Transform.class).get());
        this.camera = camera;
    }

    public void setMap(TiledMap map) {
        this.currentMap = map;
        if (map != null) {
            for (int i = 0; i < map.getLayers().getCount(); i++) {
                if (map.getLayers().get(i) instanceof TiledMapTileLayer layer) {
                    mapWidth = layer.getWidth() * layer.getTileWidth() * Lynguard.UNIT_SCALE;
                    mapHeight = layer.getHeight() * layer.getTileHeight() * Lynguard.UNIT_SCALE;
                    break;
                }
            }
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Transform transform = Transform.MAPPER.get(entity);
        Vector2 position = transform.getPosition();
        Vector2 size = transform.getSize();
        float targetX = position.x + size.x / 2f;
        float targetY = position.y + size.y / 2f;
        float newCameraX = camera.position.x + (targetX - camera.position.x) * LERP_SPEED * deltaTime;
        float newCameraY = camera.position.y + (targetY - camera.position.y) * LERP_SPEED * deltaTime;

        if (currentMap != null && mapWidth > 0 && mapHeight > 0) {
            float halfWidth = camera.viewportWidth / 2f;
            float halfHeight = camera.viewportHeight / 2f;

            if (mapWidth > camera.viewportWidth) {
                camera.position.x = MathUtils.clamp(newCameraX, halfWidth, mapWidth - halfWidth);
            } else {
                camera.position.x = mapWidth / 2f;
            }

            if (mapHeight > camera.viewportHeight) {
                camera.position.y = MathUtils.clamp(newCameraY, halfHeight, mapHeight - halfHeight);
            } else {
                camera.position.y = mapHeight / 2f;
            }
        } else {
            camera.position.x = newCameraX;
            camera.position.y = newCameraY;
        }

        camera.update();
    }
}
