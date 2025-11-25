package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

import eu.epitech.lil7_games.component.Dying;

/**
 * Removes entities that have entered the dying state after their DEATH animation finished.
 */
public class DyingSystem extends IteratingSystem {

    public interface DeathListener {
        void onDeath(Entity entity);
    }

    private DeathListener listener;

    public DyingSystem() {
        super(Family.all(Dying.class).get());
    }

    public void setDeathListener(DeathListener listener) {
        this.listener = listener;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Dying dying = Dying.MAPPER.get(entity);
        if (dying == null) return;

        eu.epitech.lil7_games.component.Animation2D animation2D = eu.epitech.lil7_games.component.Animation2D.MAPPER.get(entity);
        if (animation2D != null && animation2D.getType() == eu.epitech.lil7_games.component.Animation2D.AnimationType.DEATH) {
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim = animation2D.getAnimation();
            if (anim != null) {
                float duration = anim.getAnimationDuration();
                float state = animation2D.getStateTime();
                if (duration > 0f && state >= duration - 0.0001f) {
                    getEngine().removeEntity(entity);
                    return;
                }
            }
        }

        dying.update(deltaTime);
        if (dying.getRemainingSeconds() <= 0f) {
            if (listener != null) {
                listener.onDeath(entity);
            }
            getEngine().removeEntity(entity);
        }
    }
}
