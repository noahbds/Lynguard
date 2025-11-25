package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;

import eu.epitech.lil7_games.component.Damaged;
import eu.epitech.lil7_games.component.Dying;
import eu.epitech.lil7_games.component.Invulnerable;
import eu.epitech.lil7_games.component.Knockback;
import eu.epitech.lil7_games.component.Life;
import eu.epitech.lil7_games.component.Patrol;
import eu.epitech.lil7_games.component.Transform;
import eu.epitech.lil7_games.component.Velocity;
import eu.epitech.lil7_games.ui.model.GameViewModel;

public class DamagedSystem extends IteratingSystem {
    private final GameViewModel viewModel;

    public DamagedSystem(GameViewModel viewModel) {
        super(Family.all(Damaged.class).get());
        this.viewModel = viewModel;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Damaged damaged = Damaged.MAPPER.get(entity);
        entity.remove(Damaged.class);

        Life life = Life.MAPPER.get(entity);
        if (life != null) {
            life.addLife(-damaged.getDamage());
            entity.add(new Invulnerable(1.0f));
            if (life.getLife() <= 0f) {
                // If the entity is already dying, avoid re-triggering death animation
                if (eu.epitech.lil7_games.component.Dying.MAPPER.get(entity) != null) {
                    return;
                }
                eu.epitech.lil7_games.component.Animation2D animation2D = eu.epitech.lil7_games.component.Animation2D.MAPPER.get(entity);
                if (animation2D != null) {
                    // Only set death animation if not already set to avoid resetting stateTime
                    if (animation2D.getType() != eu.epitech.lil7_games.component.Animation2D.AnimationType.DEATH) {
                        animation2D.setType(eu.epitech.lil7_games.component.Animation2D.AnimationType.DEATH);
                        animation2D.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
                    }
                }
                Velocity velocity = Velocity.MAPPER.get(entity);
                if (velocity != null) velocity.getVelocity().setZero();
                entity.remove(Patrol.class);
                entity.add(new Dying());
                return;
            }
        }

        Transform transform = Transform.MAPPER.get(entity);
        eu.epitech.lil7_games.component.Animation2D animation2D = eu.epitech.lil7_games.component.Animation2D.MAPPER.get(entity);
        if (transform != null) {
            float knockbackForce = 7f;
            float dx = transform.getPosition().x - damaged.getSourceX();
            float dy = transform.getPosition().y - damaged.getSourceY();
            
            Vector2 knockbackDir = new Vector2(dx, dy).nor();
            knockbackDir.y = 0.5f; 
            knockbackDir.nor();
            
            Velocity velocity = Velocity.MAPPER.get(entity);
            if (velocity != null) {
                velocity.getVelocity().set(knockbackDir.scl(knockbackForce));
            }
            entity.add(new Knockback(null, 0.2f));

            if (animation2D != null) {
                animation2D.setType(eu.epitech.lil7_games.component.Animation2D.AnimationType.DEFEND);
                animation2D.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
            }

            float x = transform.getPosition().x + transform.getSize().x * 0.5f;
            float y = transform.getPosition().y;
            viewModel.playerDamage((int) damaged.getDamage(), x, y);
        }
    }
}
