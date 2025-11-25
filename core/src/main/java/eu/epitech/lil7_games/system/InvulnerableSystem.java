package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import eu.epitech.lil7_games.component.Invulnerable;
import eu.epitech.lil7_games.component.Graphic;
// import com.badlogic.gdx.graphics.Color;

public class InvulnerableSystem extends IteratingSystem {

    public InvulnerableSystem() {
        super(Family.all(Invulnerable.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Invulnerable invulnerable = Invulnerable.MAPPER.get(entity);
        invulnerable.reduceTime(deltaTime);

        Graphic graphic = Graphic.MAPPER.get(entity);
        if (graphic != null) {
            // Blink effect
            float blinkSpeed = 10f;
            boolean visible = (invulnerable.getTimeRemaining() * blinkSpeed) % 2 > 1;
            graphic.getColor().a = visible ? 0.5f : 1f;
        }

        if (invulnerable.getTimeRemaining() <= 0) {
            entity.remove(Invulnerable.class);
            if (graphic != null) {
                graphic.getColor().a = 1f;
            }
        }
    }
}
