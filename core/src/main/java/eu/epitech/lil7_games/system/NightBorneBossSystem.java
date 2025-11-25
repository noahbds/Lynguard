package eu.epitech.lil7_games.system;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

import eu.epitech.lil7_games.component.Animation2D;
import eu.epitech.lil7_games.component.Damaged;
import eu.epitech.lil7_games.component.Enemy;
import eu.epitech.lil7_games.component.Facing;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;
import eu.epitech.lil7_games.component.Velocity;
// no-op

public class NightBorneBossSystem extends IteratingSystem {
    private static final float ATTACK_RANGE = 1.6f;
    private static final float RUN_SPEED = 2.0f;
    private static final float ATTACK_COOLDOWN = 1.0f;
    private final Map<Entity, Float> attackCooldowns = new HashMap<>();

    public NightBorneBossSystem() {
        super(Family.all(Enemy.class, Animation2D.class, Transform.class, Velocity.class, Facing.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Animation2D anim = Animation2D.MAPPER.get(entity);
        if (anim == null) return;
        if (!"ennemi/NightBorneAnim".equals(anim.getAtlasKey())) return;

        if (eu.epitech.lil7_games.component.Dying.MAPPER.get(entity) != null) return;

        com.badlogic.ashley.utils.ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(Player.class, Transform.class).get());
        if (players == null || players.size() == 0) return;
        Entity player = players.get(0);
        Transform playerT = Transform.MAPPER.get(player);
        Transform t = Transform.MAPPER.get(entity);
        Velocity v = Velocity.MAPPER.get(entity);
        Facing f = Facing.MAPPER.get(entity);
        Enemy enemy = Enemy.MAPPER.get(entity);

        if (playerT == null || t == null || v == null || f == null || enemy == null) return;

        float dx = playerT.getPosition().x - t.getPosition().x;
        float dy = playerT.getPosition().y - t.getPosition().y;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);

        float cd = attackCooldowns.getOrDefault(entity, 0f);
        cd = Math.max(0f, cd - deltaTime);
        attackCooldowns.put(entity, cd);

        if (dx < -0.01f) f.setDirection(Facing.FacingDirection.LEFT);
        else if (dx > 0.01f) f.setDirection(Facing.FacingDirection.RIGHT);

        if (dist > ATTACK_RANGE) {
            anim.setType(Animation2D.AnimationType.RUN);
            anim.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            float direction = Math.signum(dx);
            v.getVelocity().x = direction * RUN_SPEED;
        } else {
            v.getVelocity().x = 0f;
            anim.setType(Animation2D.AnimationType.ATTACK);
            anim.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);

            if (cd <= 0f) {
                float dmg = enemy.getContactDamage();
                Transform et = Transform.MAPPER.get(entity);
                if (dmg > 0f && player != null && et != null) {
                    eu.epitech.lil7_games.component.Damaged existing = eu.epitech.lil7_games.component.Damaged.MAPPER.get(player);
                    if (existing == null) {
                        player.add(new Damaged(dmg, et.getPosition().x, et.getPosition().y));
                    } else {
                        existing.addDamage(dmg);
                    }
                }

                attackCooldowns.put(entity, ATTACK_COOLDOWN);
            }
        }
    }
}
