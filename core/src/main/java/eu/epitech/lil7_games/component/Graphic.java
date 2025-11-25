package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Graphic implements Component {
    public static final ComponentMapper<Graphic> MAPPER = ComponentMapper.getFor(Graphic.class);

    private TextureRegion region;
    private final Color color;
    private boolean flipX;
    private boolean flipY;

    public Graphic(Color color, TextureRegion region) {
        this(color, region, false, false);
    }

    public Graphic(Color color, TextureRegion region, boolean flipX, boolean flipY) {
        this.color = color.cpy();
        this.region = region;
        this.flipX = flipX;
        this.flipY = flipY;
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
    }

    public TextureRegion getRegion() {
        return region;
    }
    
    public Color getColor() {
        return color;
    }
    
    public boolean isFlipX() {
        return flipX;
    }
    
    public boolean isFlipY() {
        return flipY;
    }
    
    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }
    
    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }
}
