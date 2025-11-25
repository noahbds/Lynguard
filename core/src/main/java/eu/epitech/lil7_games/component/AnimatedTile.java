package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.maps.tiled.TiledMapTile;

public class AnimatedTile implements Component {
    public static final ComponentMapper<AnimatedTile> MAPPER = ComponentMapper.getFor(AnimatedTile.class);

    private final TiledMapTile tile;

    public AnimatedTile(TiledMapTile tile) {
        this.tile = tile;
    }

    public TiledMapTile getTile() {
        return tile;
    }
}
