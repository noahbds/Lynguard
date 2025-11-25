package eu.epitech.lil7_games.collision;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import eu.epitech.lil7_games.Lynguard;

public class TileCollisionSystem {
    private static final float PLATFORM_TOP_TOLERANCE = 0.01f;

    private TiledMap currentMap;

    public void setMap(TiledMap map) {
        this.currentMap = map;
    }

    public boolean checkCollision(Rectangle playerRect) {
        return checkCollision(playerRect, 0f, playerRect.y, false);
    }

    public boolean checkCollision(Rectangle playerRect, float velocityY) {
        return checkCollision(playerRect, velocityY, playerRect.y, false);
    }

    public boolean checkCollision(Rectangle playerRect, float velocityY, float previousBottom) {
        return checkCollision(playerRect, velocityY, previousBottom, false);
    }

    public boolean checkCollision(Rectangle playerRect, float velocityY, float previousBottom, boolean dropThroughPlatforms) {
        if (currentMap == null) {
            return false;
        }

        if (dropThroughPlatforms && velocityY <= 0f) {
            return false;
        }

        for (MapLayer layer : currentMap.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer tileLayer)) {
                continue;
            }

            boolean isInvisibleBarrier = "INV_BAR".equalsIgnoreCase(layer.getName());
            boolean isPlatformLayer = "PLATFORMS".equalsIgnoreCase(layer.getName());

            float tileWidth = tileLayer.getTileWidth();
            float tileHeight = tileLayer.getTileHeight();

            int leftTile = Math.max(0, (int) (playerRect.x / (tileWidth * Lynguard.UNIT_SCALE)));
            int rightTile = (int) ((playerRect.x + playerRect.width) / (tileWidth * Lynguard.UNIT_SCALE));
            int bottomTile = Math.max(0, (int) (playerRect.y / (tileHeight * Lynguard.UNIT_SCALE)));
            int topTile = (int) ((playerRect.y + playerRect.height) / (tileHeight * Lynguard.UNIT_SCALE));

            for (int row = bottomTile; row <= topTile; row++) {
                for (int col = leftTile; col <= rightTile; col++) {
                    TiledMapTileLayer.Cell cell = tileLayer.getCell(col, row);
                    if (cell == null || cell.getTile() == null) {
                        continue;
                    }

                    if (isInvisibleBarrier) {
                        return true;
                    }

                    var tileObjects = cell.getTile().getObjects();
                    if (tileObjects == null || tileObjects.getCount() == 0) {
                        continue;
                    }

                    for (MapObject tileObj : tileObjects) {
                        CollisionShape shape = createCollisionShapeForTileObject(tileObj, cell, col, row, tileWidth, tileHeight);
                        if (shape == null) {
                            continue;
                        }

                        if (shouldIgnorePlatformCollision(isPlatformLayer, shape, velocityY, previousBottom, dropThroughPlatforms)) {
                            continue;
                        }

                        if (shape.intersects(playerRect)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private CollisionShape createCollisionShapeForTileObject(MapObject tileObj, TiledMapTileLayer.Cell cell, int col, int row, float tileWidth, float tileHeight) {
        if (tileObj instanceof RectangleMapObject rectangleObject) {
            float[] localVertices = rectangleVertices(rectangleObject.getRectangle());
            float objectRotation = getObjectRotation(rectangleObject);
            if (!MathUtils.isZero(objectRotation)) {
                Polygon polygon = new Polygon(localVertices);
                Rectangle rect = rectangleObject.getRectangle();
                polygon.setOrigin(rect.x + rect.width / 2f, rect.y + rect.height / 2f);
                polygon.rotate(objectRotation);
                localVertices = polygon.getTransformedVertices();
            }

            float[] worldVertices = transformCellChildVertices(cell, col, row, localVertices, tileWidth, tileHeight);
            float totalRotation = objectRotation + cellRotationDegrees(cell);
            return shapeFromWorldVertices(worldVertices, isZeroRotation(totalRotation));
        } else if (tileObj instanceof PolygonMapObject polygonObject) {
            float[] localVertices = polygonObject.getPolygon().getTransformedVertices();
            float[] worldVertices = transformCellChildVertices(cell, col, row, localVertices, tileWidth, tileHeight);
            return CollisionShape.polygon(new Polygon(worldVertices));
        } else if (tileObj instanceof PolylineMapObject polylineObject) {
            float[] localVertices = polylineObject.getPolyline().getTransformedVertices();
            float[] worldVertices = transformCellChildVertices(cell, col, row, localVertices, tileWidth, tileHeight);
            return CollisionShape.polyline(worldVertices);
        } else if (tileObj instanceof EllipseMapObject ellipseObject) {
            Circle localCircle = createCircleFromEllipse(ellipseObject);
            Circle worldCircle = transformCellCircle(cell, col, row, localCircle, tileWidth, tileHeight);
            return CollisionShape.circle(worldCircle);
        } else if (tileObj instanceof CircleMapObject circleObject) {
            Circle localCircle = copyCircle(circleObject.getCircle());
            Circle worldCircle = transformCellCircle(cell, col, row, localCircle, tileWidth, tileHeight);
            return CollisionShape.circle(worldCircle);
        }

        return null;
    }

    private CollisionShape shapeFromWorldVertices(float[] worldVertices, boolean axisAligned) {
        if (axisAligned && worldVertices.length == 8 && isAxisAlignedRectangleVertices(worldVertices)) {
            return CollisionShape.rectangle(rectangleFromWorldVertices(worldVertices));
        }
        return CollisionShape.polygon(new Polygon(worldVertices));
    }

    private float[] rectangleVertices(Rectangle rect) {
        return new float[] {
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        };
    }

    private float[] transformCellChildVertices(TiledMapTileLayer.Cell cell, int col, int row, float[] localVertices, float tileWidth, float tileHeight) {
        boolean flipH = cell.getFlipHorizontally();
        boolean flipV = cell.getFlipVertically();
        float rotation = cellRotationDegrees(cell);
        float cos = MathUtils.cosDeg(rotation);
        float sin = MathUtils.sinDeg(rotation);

        float originX = tileWidth / 2f;
        float originY = tileHeight / 2f;
        float parentX = col * tileWidth;
        float parentY = row * tileHeight;

        float[] world = new float[localVertices.length];
        for (int i = 0; i < localVertices.length; i += 2) {
            float localX = localVertices[i];
            float localY = localVertices[i + 1];

            if (flipH) {
                localX = tileWidth - localX;
            }
            if (flipV) {
                localY = tileHeight - localY;
            }

            float relX = localX - originX;
            float relY = localY - originY;
            float rotatedX = relX * cos - relY * sin;
            float rotatedY = relX * sin + relY * cos;
            float worldX = parentX + rotatedX + originX;
            float worldY = parentY + rotatedY + originY;

            world[i] = worldX * Lynguard.UNIT_SCALE;
            world[i + 1] = worldY * Lynguard.UNIT_SCALE;
        }
        return world;
    }

    private float cellRotationDegrees(TiledMapTileLayer.Cell cell) {
        return switch (cell.getRotation()) {
            case TiledMapTileLayer.Cell.ROTATE_90 -> 90f;
            case TiledMapTileLayer.Cell.ROTATE_180 -> 180f;
            case TiledMapTileLayer.Cell.ROTATE_270 -> 270f;
            default -> 0f;
        };
    }

    private boolean shouldIgnorePlatformCollision(boolean isPlatformLayer, CollisionShape shape, float velocityY, float previousBottom, boolean dropThroughPlatforms) {
        if (!isPlatformLayer) {
            return false;
        }
        if (!isFlatPlatform(shape)) {
            return false; // sloped/complex -> always solid
        }

        if (dropThroughPlatforms) {
            return true;
        }

        Rectangle platformRect = getPlatformBounds(shape);
        if (platformRect == null) {
            return false;
        }

        if (velocityY > 0f) { // moving up through flat platform
            return true;
        }

        float platformTop = platformRect.y + platformRect.height;
        return previousBottom + PLATFORM_TOP_TOLERANCE < platformTop;
    }

    private boolean isAxisAlignedRectangleVertices(float[] vertices) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < vertices.length; i += 2) {
            float x = vertices[i];
            float y = vertices[i + 1];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        for (int i = 0; i < vertices.length; i += 2) {
            float x = vertices[i];
            float y = vertices[i + 1];
            if (!MathUtils.isEqual(x, minX, 0.01f) && !MathUtils.isEqual(x, maxX, 0.01f)) {
                return false;
            }
            if (!MathUtils.isEqual(y, minY, 0.01f) && !MathUtils.isEqual(y, maxY, 0.01f)) {
                return false;
            }
        }
        return true;
    }

    private Rectangle rectangleFromWorldVertices(float[] vertices) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 2) {
            float x = vertices[i];
            float y = vertices[i + 1];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private boolean isZeroRotation(float rotationDeg) {
        float normalized = normalizeRotation(rotationDeg);
        return MathUtils.isZero(normalized, 0.01f);
    }

    private float normalizeRotation(float rotationDeg) {
        float normalized = rotationDeg % 360f;
        if (normalized > 180f) {
            normalized -= 360f;
        } else if (normalized < -180f) {
            normalized += 360f;
        }
        return normalized;
    }

    private float getObjectRotation(MapObject object) {
        Object value = object.getProperties().get("rotation");
        if (value instanceof Float floatValue) {
            return floatValue;
        }
        if (value instanceof Double doubleValue) {
            return doubleValue.floatValue();
        }
        if (value instanceof Integer intValue) {
            return intValue.floatValue();
        }
        return 0f;
    }

    private Circle createCircleFromEllipse(EllipseMapObject ellipseObject) {
        Ellipse ellipse = ellipseObject.getEllipse();
        float centerX = ellipse.x + ellipse.width / 2f;
        float centerY = ellipse.y + ellipse.height / 2f;
        float radius = Math.max(ellipse.width, ellipse.height) / 2f;
        return new Circle(centerX, centerY, radius);
    }

    private Circle copyCircle(Circle circle) {
        return new Circle(circle.x, circle.y, circle.radius);
    }

    private Circle transformCellCircle(TiledMapTileLayer.Cell cell, int col, int row, Circle localCircle, float tileWidth, float tileHeight) {
        float[] worldCenter = transformCellChildVertices(cell, col, row, new float[] { localCircle.x, localCircle.y }, tileWidth, tileHeight);
        float radius = localCircle.radius * Lynguard.UNIT_SCALE;
        return new Circle(worldCenter[0], worldCenter[1], radius);
    }

    private Rectangle getPlatformBounds(CollisionShape shape) {
        if (shape == null) {
            return null;
        }

        return switch (shape.getType()) {
            case RECTANGLE -> shape.getRectangle();
            case POLYGON -> shape.getPolygon().getBoundingRectangle();
            case CIRCLE -> {
                Circle circle = shape.getCircle();
                float diameter = circle.radius * 2f;
                yield new Rectangle(circle.x - circle.radius, circle.y - circle.radius, diameter, diameter);
            }
            case POLYLINE -> null;
        };
    }

    private boolean isFlatPlatform(CollisionShape shape) {
        if (shape == null) return false;
        return switch (shape.getType()) {
            case RECTANGLE -> true;
            case POLYGON -> isAxisAligned(shape.getPolygon().getTransformedVertices());
            case CIRCLE, POLYLINE -> false;
        };
    }

    private boolean isAxisAligned(float[] vertices) {
        if (vertices.length != 8) return false;
        float minX = Float.MAX_VALUE; float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE; float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 2) {
            float x = vertices[i]; float y = vertices[i + 1];
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
        }
        for (int i = 0; i < vertices.length; i += 2) {
            float x = vertices[i]; float y = vertices[i + 1];
            if (!MathUtils.isEqual(x, minX, 0.01f) && !MathUtils.isEqual(x, maxX, 0.01f)) return false;
            if (!MathUtils.isEqual(y, minY, 0.01f) && !MathUtils.isEqual(y, maxY, 0.01f)) return false;
        }
        return true;
    }
}
