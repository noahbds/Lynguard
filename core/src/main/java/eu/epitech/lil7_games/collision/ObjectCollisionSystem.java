package eu.epitech.lil7_games.collision;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import eu.epitech.lil7_games.Lynguard;

public class ObjectCollisionSystem {
    private static final float PLATFORM_TOP_TOLERANCE = 0.01f;

    private static final class LayeredCollisionShape {
        private final CollisionShape shape;
        private final boolean platformLayer;

        private LayeredCollisionShape(CollisionShape shape, boolean platformLayer) {
            this.shape = shape;
            this.platformLayer = platformLayer;
        }
    }

    private final Array<LayeredCollisionShape> layeredCollisionShapes = new Array<>();
    private final Array<CollisionShape> collisionShapes = new Array<>();
    private TiledMap currentMap;

    public void setMap(TiledMap map) {
        this.currentMap = map;
        rebuildCollisionCache();
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
        if (dropThroughPlatforms && velocityY <= 0f) {
            return false;
        }
        for (LayeredCollisionShape layeredShape : layeredCollisionShapes) {
            if (layeredShape.platformLayer && shouldIgnorePlatformCollision(layeredShape.shape, playerRect, velocityY, previousBottom, dropThroughPlatforms)) {
                continue;
            }

            if (layeredShape.shape.intersects(playerRect)) {
                return true;
            }
        }
        return false;
    }

    public Array<CollisionShape> getCollisionShapes() {
        return collisionShapes;
    }

    private void rebuildCollisionCache() {
        layeredCollisionShapes.clear();
        collisionShapes.clear();
        if (currentMap == null) {
            return;
        }

        for (MapLayer layer : currentMap.getLayers()) {
            if (layer.getObjects() == null || layer.getObjects().getCount() == 0) {
                continue;
            }

            boolean platformLayer = isPlatformLayer(layer);
            for (MapObject object : layer.getObjects()) {
                addObjectCollision(object, platformLayer);
            }
        }
    }

    private boolean isPlatformLayer(MapLayer layer) {
        String layerName = layer.getName();
        return layerName != null && "PLATFORMS".equalsIgnoreCase(layerName);
    }

    private void addObjectCollision(MapObject object, boolean platformLayer) {
        if (object instanceof TiledMapTileMapObject tileMapObject) {
            cacheTileMapObjectCollision(tileMapObject, platformLayer);
            return;
        }

        if (object instanceof RectangleMapObject rectangleObject) {
            addRectangleObject(rectangleObject, platformLayer);
        } else if (object instanceof PolygonMapObject polygonObject) {
            addLayeredShape(CollisionShape.polygon(createWorldPolygon(polygonObject.getPolygon())), platformLayer);
        } else if (object instanceof PolylineMapObject polylineObject) {
            addLayeredShape(CollisionShape.polyline(createWorldPolyline(polylineObject.getPolyline())), platformLayer);
        } else if (object instanceof EllipseMapObject ellipseObject) {
            addLayeredShape(CollisionShape.circle(createCircleFromEllipse(ellipseObject)), platformLayer);
        } else if (object instanceof CircleMapObject circleObject) {
            addLayeredShape(CollisionShape.circle(createCircle(circleObject.getCircle())), platformLayer);
        }
    }

    private void cacheTileMapObjectCollision(TiledMapTileMapObject tileObject, boolean platformLayer) {
        if (tileObject.getTile() == null) {
            return;
        }

        var tileObjects = tileObject.getTile().getObjects();
        if (tileObjects == null || tileObjects.getCount() == 0) {
            return;
        }

        for (MapObject child : tileObjects) {
            if (child instanceof RectangleMapObject rectangleObject) {
                Rectangle rect = rectangleObject.getRectangle();
                float[] localVertices = rectangleVertices(rect);
                float[] worldVertices = transformTileChildVertices(tileObject, localVertices);
                float totalRotation = tileObject.getRotation() + getObjectRotation(child);
                addWorldVertices(worldVertices, isZeroRotation(totalRotation), platformLayer);
            } else if (child instanceof PolygonMapObject polygonObject) {
                float[] localVertices = polygonObject.getPolygon().getTransformedVertices();
                addLayeredShape(CollisionShape.polygon(new Polygon(transformTileChildVertices(tileObject, localVertices))), platformLayer);
            } else if (child instanceof PolylineMapObject polylineObject) {
                float[] localVertices = polylineObject.getPolyline().getTransformedVertices();
                addLayeredShape(CollisionShape.polyline(transformTileChildVertices(tileObject, localVertices)), platformLayer);
            } else if (child instanceof EllipseMapObject ellipseObject) {
                addLayeredShape(CollisionShape.circle(transformTileEllipse(tileObject, ellipseObject.getEllipse())), platformLayer);
            } else if (child instanceof CircleMapObject circleObject) {
                addLayeredShape(CollisionShape.circle(transformTileCircle(tileObject, circleObject.getCircle())), platformLayer);
            }
        }
    }

    private void addLayeredShape(CollisionShape shape, boolean platformLayer) {
        layeredCollisionShapes.add(new LayeredCollisionShape(shape, platformLayer));
        collisionShapes.add(shape);
    }

    private void addRectangleObject(RectangleMapObject rectObject, boolean platformLayer) {
        Rectangle rect = rectObject.getRectangle();
        float rotation = getObjectRotation(rectObject);
        if (isZeroRotation(rotation)) {
            addLayeredShape(CollisionShape.rectangle(scaleRectangle(rect)), platformLayer);
        } else {
            addLayeredShape(CollisionShape.polygon(createRotatedRectanglePolygon(rect, rotation)), platformLayer);
        }
    }

    private Rectangle scaleRectangle(Rectangle rect) {
        return new Rectangle(
            rect.x * Lynguard.UNIT_SCALE,
            rect.y * Lynguard.UNIT_SCALE,
            rect.width * Lynguard.UNIT_SCALE,
            rect.height * Lynguard.UNIT_SCALE
        );
    }

    private Polygon createRotatedRectanglePolygon(Rectangle rect, float rotationDeg) {
        Polygon polygon = new Polygon(new float[] {
            0f, 0f,
            rect.width, 0f,
            rect.width, rect.height,
            0f, rect.height
        });
        polygon.setOrigin(0f, 0f);
        polygon.setPosition(rect.x, rect.y);
        polygon.setRotation(rotationDeg);
        return new Polygon(scaleVertices(polygon.getTransformedVertices()));
    }

    private Polygon createWorldPolygon(Polygon polygon) {
        return new Polygon(scaleVertices(polygon.getTransformedVertices()));
    }

    private float[] createWorldPolyline(Polyline polyline) {
        return scaleVertices(polyline.getTransformedVertices());
    }

    private Circle createCircleFromEllipse(EllipseMapObject ellipseObject) {
        Ellipse ellipse = ellipseObject.getEllipse();
        float centerX = (ellipse.x + ellipse.width / 2f) * Lynguard.UNIT_SCALE;
        float centerY = (ellipse.y + ellipse.height / 2f) * Lynguard.UNIT_SCALE;
        float radius = Math.max(ellipse.width, ellipse.height) / 2f * Lynguard.UNIT_SCALE;
        return new Circle(centerX, centerY, radius);
    }

    private Circle createCircle(Circle circle) {
        float scale = Lynguard.UNIT_SCALE;
        return new Circle(circle.x * scale, circle.y * scale, circle.radius * scale);
    }

    private float[] rectangleVertices(Rectangle rect) {
        return new float[] {
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        };
    }

    private float[] transformTileChildVertices(TiledMapTileMapObject parent, float[] localVertices) {
        boolean flipH = parent.isFlipHorizontally();
        boolean flipV = parent.isFlipVertically();
        float scaleX = parent.getScaleX();
        float scaleY = parent.getScaleY();
        if (MathUtils.isZero(scaleX)) scaleX = 1f;
        if (MathUtils.isZero(scaleY)) scaleY = 1f;

        float originX = parent.getOriginX();
        float originY = parent.getOriginY();
        float rotation = parent.getRotation();
        float cos = MathUtils.cosDeg(rotation);
        float sin = MathUtils.sinDeg(rotation);
        float tileWidth = parent.getTile().getTextureRegion().getRegionWidth();
        float tileHeight = parent.getTile().getTextureRegion().getRegionHeight();

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

            localX *= scaleX;
            localY *= scaleY;

            float scaledOriginX = originX * scaleX;
            float scaledOriginY = originY * scaleY;
            float relX = localX - scaledOriginX;
            float relY = localY - scaledOriginY;
            float rotatedX = relX * cos - relY * sin;
            float rotatedY = relX * sin + relY * cos;
            float worldX = parent.getX() + rotatedX + scaledOriginX;
            float worldY = parent.getY() + rotatedY + scaledOriginY;

            world[i] = worldX * Lynguard.UNIT_SCALE;
            world[i + 1] = worldY * Lynguard.UNIT_SCALE;
        }
        return world;
    }

    private void addWorldVertices(float[] worldVertices, boolean axisAligned, boolean platformLayer) {
        if (axisAligned && worldVertices.length == 8 && isAxisAlignedRectangleVertices(worldVertices)) {
            addLayeredShape(CollisionShape.rectangle(rectangleFromWorldVertices(worldVertices)), platformLayer);
        } else {
            addLayeredShape(CollisionShape.polygon(new Polygon(worldVertices)), platformLayer);
        }
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

    private float[] scaleVertices(float[] vertices) {
        float[] scaled = new float[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            scaled[i] = vertices[i] * Lynguard.UNIT_SCALE;
        }
        return scaled;
    }

    private Circle transformTileEllipse(TiledMapTileMapObject parent, Ellipse ellipse) {
        float centerX = ellipse.x + ellipse.width / 2f;
        float centerY = ellipse.y + ellipse.height / 2f;
        float[] worldCenter = transformTileChildVertices(parent, new float[] { centerX, centerY });

        float scaleX = parent.getScaleX();
        float scaleY = parent.getScaleY();
        if (MathUtils.isZero(scaleX)) scaleX = 1f;
        if (MathUtils.isZero(scaleY)) scaleY = 1f;

        float radius = Math.max(ellipse.width * Math.abs(scaleX), ellipse.height * Math.abs(scaleY)) / 2f * Lynguard.UNIT_SCALE;
        return new Circle(worldCenter[0], worldCenter[1], radius);
    }

    private Circle transformTileCircle(TiledMapTileMapObject parent, Circle circle) {
        float[] worldCenter = transformTileChildVertices(parent, new float[] { circle.x, circle.y });

        float scaleX = parent.getScaleX();
        float scaleY = parent.getScaleY();
        if (MathUtils.isZero(scaleX)) scaleX = 1f;
        if (MathUtils.isZero(scaleY)) scaleY = 1f;

        float scale = Math.max(Math.abs(scaleX), Math.abs(scaleY));
        float radius = circle.radius * scale * Lynguard.UNIT_SCALE;
        return new Circle(worldCenter[0], worldCenter[1], radius);
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

    private Rectangle getPlatformBounds(CollisionShape shape) {
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

    private boolean shouldIgnorePlatformCollision(CollisionShape platformShape, Rectangle playerRect, float velocityY, float previousBottom, boolean dropThroughPlatforms) {
        // Only ignore collision for genuinely flat one-way platforms (axis-aligned rectangles).
        if (!isFlatPlatform(platformShape)) {
            return false; // sloped or complex -> always solid
        }

        if (dropThroughPlatforms) {
            return true;
        }

        if (velocityY > 0f) { // moving up through a flat platform
            return true;
        }

        Rectangle platformBounds = getPlatformBounds(platformShape);
        if (platformBounds == null) {
            return false;
        }

        float platformTop = platformBounds.y + platformBounds.height;
        return previousBottom + PLATFORM_TOP_TOLERANCE < platformTop;
    }

    private boolean isFlatPlatform(CollisionShape shape) {
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
