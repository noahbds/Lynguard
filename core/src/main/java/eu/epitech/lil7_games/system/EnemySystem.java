package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import eu.epitech.lil7_games.component.Animation2D;
import eu.epitech.lil7_games.component.Animation2D.AnimationType;
import eu.epitech.lil7_games.component.Collider;
import eu.epitech.lil7_games.component.Damaged;
import eu.epitech.lil7_games.component.Defense;
import eu.epitech.lil7_games.component.Enemy;
import eu.epitech.lil7_games.component.Facing;
import eu.epitech.lil7_games.component.Flip;
import eu.epitech.lil7_games.component.Patrol;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;
import eu.epitech.lil7_games.component.Velocity;
import eu.epitech.lil7_games.component.Invulnerable;

public class EnemySystem extends IteratingSystem {
    private static final float MIN_SPEED_THRESHOLD = 0.01f;

    private final Family playerFamily;
    private final Rectangle enemyBounds;
    private final Rectangle playerBounds;

    private ImmutableArray<Entity> players;

    public EnemySystem() {
        super(Family.all(Enemy.class, Transform.class).get());
        this.playerFamily = Family.all(Player.class, Transform.class).get();
        this.enemyBounds = new Rectangle();
        this.playerBounds = new Rectangle();
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.players = engine.getEntitiesFor(playerFamily);
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
        this.players = null;
    }

    @Override
    protected void processEntity(Entity enemyEntity, float deltaTime) {
        Enemy enemy = Enemy.MAPPER.get(enemyEntity);
        Transform transform = Transform.MAPPER.get(enemyEntity);
        Velocity velocity = Velocity.MAPPER.get(enemyEntity);
        Patrol patrol = Patrol.MAPPER.get(enemyEntity);
        Facing facing = Facing.MAPPER.get(enemyEntity);
        Flip flip = Flip.MAPPER.get(enemyEntity);
        Animation2D animation = Animation2D.MAPPER.get(enemyEntity);

        if (patrol != null) {
            updatePatrol(transform, velocity, patrol, facing, flip, animation, deltaTime);
        }

        enemy.tickCooldown(deltaTime);
        if (enemy.getCooldownRemaining() > 0f) {
            return;
        }

        if (players == null || players.size() == 0) {
            return;
        }

        Collider enemyCollider = Collider.MAPPER.get(enemyEntity);
        computeBounds(enemyBounds, transform, enemyCollider);

        for (int i = 0; i < players.size(); i++) {
            Entity player = players.get(i);
            if (player == enemyEntity) {
                continue;
            }

            Transform playerTransform = Transform.MAPPER.get(player);
            if (playerTransform == null) {
                continue;
            }

            Collider playerCollider = Collider.MAPPER.get(player);
            computeBounds(playerBounds, playerTransform, playerCollider);

            if (!enemyBounds.overlaps(playerBounds)) {
                continue;
            }

            if (Invulnerable.MAPPER.has(player)) {
                continue;
            }

            Defense defense = Defense.MAPPER.get(player);
            if (defense != null && defense.isDefending()) {
                continue;
            }

            float damage = Math.max(0f, enemy.getContactDamage());
            if (damage <= 0f) {
                continue;
            }

            Damaged damaged = Damaged.MAPPER.get(player);
            if (damaged == null) {
                player.add(new Damaged(damage, transform.getPosition().x, transform.getPosition().y));
            } else {
                damaged.addDamage(damage);
            }

            enemy.triggerContactCooldown();
            break;
        }
    }

    private void updatePatrol(Transform transform,
                              Velocity velocity,
                              Patrol patrol,
                              Facing facing,
                              Flip flip,
                              Animation2D animation,
                              float deltaTime) {
        Vector2 position = transform.getPosition();
        Vector2 vel = velocity != null ? velocity.getVelocity() : null;

        float direction = patrol.isMovingRight() ? 1f : -1f;
        float speed = patrol.getSpeed();
        float deltaX = speed * direction * deltaTime;
        float nextX = position.x + deltaX;

        float leftBound = patrol.getLeftBound();
        float rightBound = patrol.getRightBound();

        if (patrol.isMovingRight() && nextX >= rightBound) {
            nextX = rightBound;
            patrol.setMovingRight(false);
            direction = -1f;
        } else if (!patrol.isMovingRight() && nextX <= leftBound) {
            nextX = leftBound;
            patrol.setMovingRight(true);
            direction = 1f;
        }

        position.x = nextX;
        if (vel != null) {
            vel.x = speed * direction;
        }

        if (facing != null) {
            facing.setDirection(direction >= 0f ? Facing.FacingDirection.RIGHT : Facing.FacingDirection.LEFT);
        }

        if (flip != null) {
            flip.setFlipX(direction < 0f);
        }

        if (animation != null) {
            float absSpeed = Math.abs(speed * direction);
            if (absSpeed > MIN_SPEED_THRESHOLD) {
                if (animation.getType() != AnimationType.WALK) {
                    animation.setType(AnimationType.WALK);
                }
            } else if (animation.getType() != AnimationType.IDLE) {
                animation.setType(AnimationType.IDLE);
            }
        }
    }

    private void computeBounds(Rectangle out, Transform transform, Collider collider) {
        Vector2 position = transform.getPosition();
        Vector2 size = transform.getSize();
        if (collider != null) {
            out.set(position.x + collider.getOffsetX(),
                    position.y + collider.getOffsetY(),
                    collider.getWidth(),
                    collider.getHeight());
        } else {
            out.set(position.x, position.y, size.x, size.y);
        }
    }
}
