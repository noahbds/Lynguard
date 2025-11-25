package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import eu.epitech.lil7_games.component.AnimatedTile;
import eu.epitech.lil7_games.component.Graphic;

public class AnimatedTileSystem extends IteratingSystem {

    public AnimatedTileSystem() {
        super(Family.all(AnimatedTile.class, Graphic.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AnimatedTile animatedTile = AnimatedTile.MAPPER.get(entity);
        Graphic graphic = Graphic.MAPPER.get(entity);

        graphic.setRegion(animatedTile.getTile().getTextureRegion());
    }
}
