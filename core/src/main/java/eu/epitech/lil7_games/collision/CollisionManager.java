package eu.epitech.lil7_games.collision;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class CollisionManager {
    private final TileCollisionSystem tileCollisionSystem;
    private final ObjectCollisionSystem objectCollisionSystem;

    public CollisionManager() {
        this.tileCollisionSystem = new TileCollisionSystem();
        this.objectCollisionSystem = new ObjectCollisionSystem();
    }

    public void setMap(TiledMap map) {
        tileCollisionSystem.setMap(map);
        objectCollisionSystem.setMap(map);
    }

    public boolean checkCollision(Rectangle playerRect) {
        return checkCollision(playerRect, 0f, playerRect.y, false);
    }

    public boolean checkCollision(Rectangle playerRect, float velocityY) {
        return checkCollision(playerRect, velocityY, playerRect.y, false);
    }

    public boolean checkCollision(Rectangle playerRect, float velocityY, float previousBottomY) {
        return checkCollision(playerRect, velocityY, previousBottomY, false);
    }

    public boolean checkCollision(Rectangle playerRect, float velocityY, float previousBottomY, boolean dropThroughPlatforms) {
        if (tileCollisionSystem.checkCollision(playerRect, velocityY, previousBottomY, dropThroughPlatforms)) {
            return true;
        }

        return objectCollisionSystem.checkCollision(playerRect, velocityY, previousBottomY, dropThroughPlatforms);
    }

    public Array<CollisionShape> getObjectCollisionShapes() {
        return objectCollisionSystem.getCollisionShapes();
    }

    public TileCollisionSystem getTileCollisionSystem() {
        return tileCollisionSystem;
    }

    public ObjectCollisionSystem getObjectCollisionSystem() {
        return objectCollisionSystem;
    }
}
