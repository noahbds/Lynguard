package eu.epitech.lil7_games.collision;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

public class CollisionShape {
    public enum Type {
        RECTANGLE,
        POLYGON,
        CIRCLE,
        POLYLINE
    }

    private final Type type;
    private Rectangle rectangle;
    private Polygon polygon;
    private Circle circle;
    private float[] polylineVertices;

    private CollisionShape(Type type) {
        this.type = type;
    }

    public static CollisionShape rectangle(Rectangle rect) {
        CollisionShape shape = new CollisionShape(Type.RECTANGLE);
        shape.rectangle = rect;
        return shape;
    }

    public static CollisionShape polygon(Polygon polygon) {
        CollisionShape shape = new CollisionShape(Type.POLYGON);
        shape.polygon = polygon;
        return shape;
    }

    public static CollisionShape circle(Circle circle) {
        CollisionShape shape = new CollisionShape(Type.CIRCLE);
        shape.circle = circle;
        return shape;
    }

    public static CollisionShape polyline(float[] vertices) {
        CollisionShape shape = new CollisionShape(Type.POLYLINE);
        shape.polylineVertices = vertices;
        return shape;
    }

    public Type getType() {
        return type;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public Circle getCircle() {
        return circle;
    }

    public float[] getPolylineVertices() {
        return polylineVertices;
    }

    public boolean intersects(Rectangle playerRect) {
        return switch (type) {
            case RECTANGLE -> playerRect.overlaps(rectangle);
            case POLYGON -> {
                Polygon playerPolygon = rectangleToPolygon(playerRect);
                yield Intersector.overlapConvexPolygons(playerPolygon, polygon);
            }
            case CIRCLE -> Intersector.overlaps(circle, playerRect);
            case POLYLINE -> intersectsPolyline(polylineVertices, playerRect);
        };
    }

    public static Polygon rectangleToPolygon(Rectangle rect) {
        return new Polygon(new float[] {
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        });
    }

    public static boolean intersectsPolyline(float[] vertices, Rectangle rect) {
        if (vertices.length < 4) {
            return false;
        }
        for (int i = 0; i <= vertices.length - 4; i += 2) {
            float x1 = vertices[i];
            float y1 = vertices[i + 1];
            float x2 = vertices[i + 2];
            float y2 = vertices[i + 3];
            if (Intersector.intersectSegmentRectangle(x1, y1, x2, y2, rect)) {
                return true;
            }
        }
        return false;
    }
}
