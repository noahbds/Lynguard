package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Flip implements Component {
    public static final ComponentMapper<Flip> MAPPER = ComponentMapper.getFor(Flip.class);

    private boolean flipX;
    private boolean flipY;

    public Flip() {
        this(false, false);
    }

    public Flip(boolean flipX, boolean flipY) {
        this.flipX = flipX;
        this.flipY = flipY;
    }

    public boolean isFlipX() {
        return flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean isFlipY() {
        return flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }
}
