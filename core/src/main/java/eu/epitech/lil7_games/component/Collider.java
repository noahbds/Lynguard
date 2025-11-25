package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Collider stores a separate hitbox (width/height) and optional offset relative
 * to the entity's Transform position. This lets you keep a larger visual sprite
 * while using a tighter collision box aligned to the feet.
 */
public class Collider implements Component {
    public static final ComponentMapper<Collider> MAPPER = ComponentMapper.getFor(Collider.class);

    private float width;
    private float height;
    private float offsetX;
    private float offsetY;

    public Collider(float width, float height) {
        this(width, height, 0f, 0f);
    }

    public Collider(float width, float height, float offsetX, float offsetY) {
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }

    public void setWidth(float width) { this.width = width; }
    public void setHeight(float height) { this.height = height; }
    public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
}
