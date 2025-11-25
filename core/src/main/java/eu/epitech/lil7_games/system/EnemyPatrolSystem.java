package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import eu.epitech.lil7_games.component.Enemy;
import eu.epitech.lil7_games.component.Flip;
import eu.epitech.lil7_games.component.Patrol;
import eu.epitech.lil7_games.component.Transform;

public class EnemyPatrolSystem extends IteratingSystem {
    public EnemyPatrolSystem() {
        super(Family.all(Enemy.class, Patrol.class, Transform.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Patrol patrol = Patrol.MAPPER.get(entity);
        Transform transform = Transform.MAPPER.get(entity);
        Vector2 position = transform.getPosition();
        Vector2 size = transform.getSize();

        float direction = patrol.isMovingRight() ? 1f : -1f;
        position.x += direction * patrol.getSpeed() * deltaTime;

        float minX = patrol.getLeftBound();
        float maxX = patrol.getRightBound() - size.x;

        if (position.x <= minX) {
            position.x = minX;
            patrol.setMovingRight(true);
        } else if (position.x >= maxX) {
            position.x = maxX;
            patrol.setMovingRight(false);
        }

        Flip flip = Flip.MAPPER.get(entity);
        if (flip != null) {
            flip.setFlipX(!patrol.isMovingRight());
        }
    }
}
