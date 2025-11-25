package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.Color;

/**
 * Marks an entity to be drawn with a colored outline by the RenderSystem.
 * thickness is expressed in world units (game units) as a small offset used to draw
 * several copies of the texture behind the main sprite.
 */
public class Outline implements Component {
    public static final ComponentMapper<Outline> MAPPER = ComponentMapper.getFor(Outline.class);

    private Color color;
    private float thickness;

    public Outline(Color color, float thickness) {
        this.color = color;
        this.thickness = thickness;
    }

    public Color getColor() { return color; }
    public float getThickness() { return thickness; }

    public void setColor(Color color) { this.color = color; }
    public void setThickness(float thickness) { this.thickness = thickness; }
}
