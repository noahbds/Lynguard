package eu.epitech.lil7_games.system;

import java.util.Locale;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.SoundAsset;
import eu.epitech.lil7_games.audio.AudioService;
import eu.epitech.lil7_games.collision.CollisionManager;
import eu.epitech.lil7_games.component.Attack;
import eu.epitech.lil7_games.component.Collider;
import eu.epitech.lil7_games.component.Dash;
import eu.epitech.lil7_games.component.Defense;
import eu.epitech.lil7_games.component.Dying;
import eu.epitech.lil7_games.component.Knockback;
import eu.epitech.lil7_games.component.NoGravity;
import eu.epitech.lil7_games.component.Physics;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;
import eu.epitech.lil7_games.component.Velocity;
import eu.epitech.lil7_games.dash.DashController;

public class PlayerSystem extends IteratingSystem {
    private static final float MOVE_SPEED = 3.0f;
    private static final float BASE_DASH_SPEED = 10.0f;
    private static final float DASH_SPEED_PER_LEVEL = 7.5f;
    private static final float DASH_DURATION = 0.25f;
    private static final float DASH_COOLDOWN = 1.0f;
    private static final float MAX_DASH_CHARGE_TIME = 5.0f;
    private static final float DASH_HOLD_THRESHOLD = 0.5f;
    private static final float JUMP_VELOCITY = 8.0f;
    private static final float GRAVITY = -15.0f;
    private static final float MAX_FALL_SPEED = -10.0f;
    private static final float STEP_HEIGHT = 0.25f;
    private static final float DROP_THROUGH_GRACE = 0.3f;
    private final CollisionManager collisionManager;
    private float mapWidth;
    private float mapHeight;
    private TiledMap currentMap;
    private float dropThroughTimer;
    private AudioService audioService;

    public PlayerSystem() {
    super(Family.all(Player.class, Dash.class, Transform.class, Velocity.class, Physics.class, Attack.class, Defense.class).get());
        this.collisionManager = new CollisionManager();
        this.mapWidth = 0;
        this.mapHeight = 0;
        this.currentMap = null;
        this.dropThroughTimer = 0f;
    }

    public void setAudioService(AudioService audioService) {
        this.audioService = audioService;
    }

    public void setMap(TiledMap map) {
        this.currentMap = map;
        collisionManager.setMap(map);

        if (map == null) {
            this.mapWidth = 0f;
            this.mapHeight = 0f;
            return;
        }

        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        int width = map.getProperties().get("width", Integer.class);
        int height = map.getProperties().get("height", Integer.class);

        this.mapWidth = (width * tileWidth) * 0.0625f;
        this.mapHeight = (height * tileHeight) * 0.0625f;
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
    Transform transform = Transform.MAPPER.get(entity);
        Velocity velocity = Velocity.MAPPER.get(entity);
        Physics physics = Physics.MAPPER.get(entity);
        NoGravity noGravity = NoGravity.MAPPER.get(entity);
        if (noGravity != null) {
            noGravity.update(deltaTime);
            if (noGravity.getRemainingSeconds() <= 0f) {
                // When the anti-gravity effect ends we remove the component and start the cooldown
                entity.remove(NoGravity.class);
                // start cooldown after effect end (1.5s)
                entity.add(new eu.epitech.lil7_games.component.NoGravityCooldown(1.5f));
                noGravity = null;
            }
        }

        eu.epitech.lil7_games.component.NoGravityCooldown cooldown = eu.epitech.lil7_games.component.NoGravityCooldown.MAPPER.get(entity);
        if (cooldown != null) {
            cooldown.update(deltaTime);
            if (cooldown.getRemainingSeconds() <= 0f) {
                entity.remove(eu.epitech.lil7_games.component.NoGravityCooldown.class);
            }
        }
        Dash dash = Dash.MAPPER.get(entity);
    Collider collider = Collider.MAPPER.get(entity);
    Attack attack = Attack.MAPPER.get(entity);
    Defense defense = Defense.MAPPER.get(entity);
        eu.epitech.lil7_games.component.Facing facing = eu.epitech.lil7_games.component.Facing.MAPPER.get(entity);
        eu.epitech.lil7_games.component.Animation2D animation2D = eu.epitech.lil7_games.component.Animation2D.MAPPER.get(entity);

        updateDropThroughTimer(deltaTime);

        if (attack != null) attack.update(deltaTime);
        if (defense != null) defense.update(deltaTime);

        Knockback knockback = Knockback.MAPPER.get(entity);
        Dying dying = Dying.MAPPER.get(entity);
        if (knockback != null) {
            knockback.reduceDuration(deltaTime);
            if (knockback.getDuration() <= 0) {
                entity.remove(Knockback.class);
            }
        } else if (dying != null) {
            if (velocity != null) {
                velocity.getVelocity().setZero();
            }
        } else {
            handleInput(transform, collider, velocity, physics, dash, attack, defense, deltaTime);
        }

        updateDashState(dash, velocity, deltaTime);
        if (!dash.isCharging() && !dash.isDashing()) {
            if (noGravity == null) {
                applyGravity(velocity, deltaTime);
            }
        }
    updatePosition(transform, collider, velocity, physics, deltaTime);

        if (facing != null) {
            if (velocity.getVelocity().x < -0.01f) facing.setDirection(eu.epitech.lil7_games.component.Facing.FacingDirection.LEFT);
            else if (velocity.getVelocity().x > 0.01f) facing.setDirection(eu.epitech.lil7_games.component.Facing.FacingDirection.RIGHT);
            eu.epitech.lil7_games.component.Flip flip = eu.epitech.lil7_games.component.Flip.MAPPER.get(entity);
            if (flip != null) flip.setFlipX(facing.getDirection() == eu.epitech.lil7_games.component.Facing.FacingDirection.LEFT);
        }

        if (animation2D != null) {
            if (dying != null) {
                if (animation2D.getType() != eu.epitech.lil7_games.component.Animation2D.AnimationType.DEATH) {
                    animation2D.setType(eu.epitech.lil7_games.component.Animation2D.AnimationType.DEATH);
                    animation2D.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
                }
            } else {
            eu.epitech.lil7_games.component.Animation2D.AnimationType newType;
            if (defense != null && defense.isDefending()) {
                newType = eu.epitech.lil7_games.component.Animation2D.AnimationType.DEFEND;
            } else if (attack != null && attack.isAttacking()) {
                animation2D.setAttackComboStage(attack.getComboStage());
                newType = eu.epitech.lil7_games.component.Animation2D.AnimationType.ATTACK;
            } else if (dash.isDashing()) {
                newType = eu.epitech.lil7_games.component.Animation2D.AnimationType.RUN;
            } else if (!physics.isOnGround()) {
                newType = eu.epitech.lil7_games.component.Animation2D.AnimationType.JUMP;
            } else {
                com.badlogic.gdx.math.Vector2 v = (velocity != null) ? velocity.getVelocity() : null;
                float absX = (v != null) ? Math.abs(v.x) : 0f;
                if (absX > MOVE_SPEED + 0.01f) {
                    newType = eu.epitech.lil7_games.component.Animation2D.AnimationType.RUN;
                } else if (absX > 0.05f) {
                    newType = eu.epitech.lil7_games.component.Animation2D.AnimationType.WALK;
                } else {
                    newType = eu.epitech.lil7_games.component.Animation2D.AnimationType.IDLE;
                }
            }
                if (animation2D.getType() != newType) {
                    animation2D.setType(newType);
                    if (newType == eu.epitech.lil7_games.component.Animation2D.AnimationType.ATTACK || newType == eu.epitech.lil7_games.component.Animation2D.AnimationType.DEFEND) {
                        animation2D.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
                    } else {
                        animation2D.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
                    }
                }
                if (attack != null && attack.isAttacking() && animation2D.getType() == eu.epitech.lil7_games.component.Animation2D.AnimationType.ATTACK && animation2D.isFinished()) {
                    attack.stopAttack();
                }
                if (defense != null && defense.isDefending() && animation2D.getType() == eu.epitech.lil7_games.component.Animation2D.AnimationType.DEFEND && animation2D.isFinished()) {
                    defense.stopDefense();
                }
            }
        }

        if (attack != null && attack.isAttacking() && animation2D != null && animation2D.getType() == eu.epitech.lil7_games.component.Animation2D.AnimationType.ATTACK && !attack.hasHitRegistered()) {
            float attackWidth = 1.0f;
            float attackHeight;
            float attackX;
            float attackY = transform.getPosition().y + (collider != null ? collider.getOffsetY() : 0f);

            if (collider != null) {
                attackHeight = collider.getHeight();
            } else {
                attackHeight = transform.getSize().y * transform.getScaling().y;
            }

            boolean facingRight = true;
            if (facing != null) facingRight = facing.getDirection() == eu.epitech.lil7_games.component.Facing.FacingDirection.RIGHT;

            if (facingRight) {
                float baseX = transform.getPosition().x + (collider != null ? collider.getOffsetX() + collider.getWidth() : transform.getSize().x * transform.getScaling().x);
                attackX = baseX;
            } else {
                float baseX = transform.getPosition().x + (collider != null ? collider.getOffsetX() : 0f);
                attackX = baseX - attackWidth;
            }

            Rectangle attackRect = new Rectangle(attackX, attackY, attackWidth, attackHeight);

            ImmutableArray<Entity> enemies = getEngine().getEntitiesFor(Family.all(eu.epitech.lil7_games.component.Enemy.class, Transform.class).get());
            int applied = 0;
            for (int i = 0; i < enemies.size(); i++) {
                Entity e = enemies.get(i);
                if (e == entity) continue;
                Transform et = Transform.MAPPER.get(e);
                if (et == null) continue;
                Collider ec = Collider.MAPPER.get(e);
                Rectangle eb = new Rectangle();
                if (ec != null) {
                    eb.set(et.getPosition().x + ec.getOffsetX(), et.getPosition().y + ec.getOffsetY(), ec.getWidth(), ec.getHeight());
                } else {
                    eb.set(et.getPosition().x, et.getPosition().y, et.getSize().x * et.getScaling().x, et.getSize().y * et.getScaling().y);
                }
                if (attackRect.overlaps(eb)) {
                    float dmg = 1f;
                    eu.epitech.lil7_games.component.Damaged existing = eu.epitech.lil7_games.component.Damaged.MAPPER.get(e);
                    if (existing == null) {
                        e.add(new eu.epitech.lil7_games.component.Damaged(dmg, transform.getPosition().x, transform.getPosition().y));
                    } else {
                        existing.addDamage(dmg);
                    }
                    applied++;
                }
            }
            if (applied > 0) {
                attack.setHitRegistered(true);
            }
        }
    }

    private void handleInput(Transform transform, Collider collider, Velocity velocity, Physics physics, Dash dash, Attack attack, Defense defense, float deltaTime) {
        Vector2 vel = velocity.getVelocity();

        boolean lockMovement = (attack != null && attack.isAttacking()) || (defense != null && defense.isDefending());
        if (lockMovement) {
            vel.x = 0f;
        }

        if (!dash.isCharging() && !dash.isDashing() && Math.abs(vel.x) > 0.0001f) {
            dash.setLastDirection(new Vector2(Math.signum(vel.x), 0f));
        }

        if (dash.getCooldownRemaining() > 0f) {
            dash.setCooldownRemaining(dash.getCooldownRemaining() - deltaTime);
        }

        boolean spacePressed = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (!dash.isCharging() && !dash.isDashing() && !lockMovement) {
            if (spacePressed) {
                dash.setHoldTime(dash.getHoldTime() + deltaTime);
                if (dash.getCooldownRemaining() <= 0f && dash.getHoldTime() >= DASH_HOLD_THRESHOLD) {
                    dash.setCharging(true);
                    dash.setChargeTime(0f);
                    dash.setChargeLevel(1);
                    dash.setHoldTime(0f);
                }
            } else {
                if (dash.getHoldTime() > 0f && dash.getHoldTime() < DASH_HOLD_THRESHOLD && physics.isOnGround()) {
                    vel.y = JUMP_VELOCITY;
                    physics.setOnGround(false);
                }
                dash.setHoldTime(0f);
            }
        }

        if (dash.isCharging()) {
            if (dash.getChargeLevel() < DashController.MAX_LEVEL) {
                float newChargeTime = Math.min(MAX_DASH_CHARGE_TIME, dash.getChargeTime() + deltaTime);
                dash.setChargeTime(newChargeTime);
                dash.setChargeLevel(DashController.computeChargeLevel(newChargeTime, MAX_DASH_CHARGE_TIME));
                if (dash.getChargeLevel() >= DashController.MAX_LEVEL) {
                    dash.setChargeLevel(DashController.MAX_LEVEL);
                    dash.setChargeTime(MAX_DASH_CHARGE_TIME);
                }
            } else {
                dash.setChargeLevel(DashController.MAX_LEVEL);
                dash.setChargeTime(MAX_DASH_CHARGE_TIME);
            }
            vel.setZero();

            if (!spacePressed) {
                triggerDash(dash, velocity);
            }
            return;
        }

        if (!dash.isDashing() && !lockMovement) {
            boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.A);
            boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
            boolean sprint = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            float targetSpeed = sprint ? MOVE_SPEED * 1.75f : MOVE_SPEED;
            if (left && !right) {
                vel.x = -targetSpeed;
            } else if (right && !left) {
                vel.x = targetSpeed;
            } else {
                vel.x = 0f;
            }

            if ((Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.Z)) && physics.isOnGround()) {
                float effectiveJump = JUMP_VELOCITY;
                Rectangle playerRect;
                if (collider != null) {
                    playerRect = new Rectangle(transform.getPosition().x + collider.getOffsetX(), transform.getPosition().y + collider.getOffsetY(), collider.getWidth(), collider.getHeight());
                } else {
                    playerRect = new Rectangle(transform.getPosition().x, transform.getPosition().y, transform.getSize().x * transform.getScaling().x, transform.getSize().y * transform.getScaling().y);
                }

                if (isPlayerInsideNamedLayer(playerRect, "JUMP_BOOST")) {
                    effectiveJump = 10.0f;
                }

                vel.y = effectiveJump;
                physics.setOnGround(false);
                if (this.audioService != null) {
                    try {
                        float louder = Math.min(1.0f, this.audioService.getSoundVolume() * 2.0f);
                        this.audioService.playSound(SoundAsset.JUMP, louder);
                    } catch (Exception ignored) {
                    }
                }
            }

            boolean attackPressed = Gdx.input.isKeyJustPressed(Input.Keys.F) || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
            if (attack != null && attackPressed && !attack.isAttacking() && attack.getCooldownRemaining() <= 0f) {
                attack.advanceCombo();
                attack.startAttack();
                if (vel.x == 0f) {
                    vel.x = (right ? 1f : left ? -1f : 0f) * targetSpeed * 0.5f;
                }
                    if (this.audioService != null) {
                        try {
                            this.audioService.playSound(SoundAsset.SWORDSLASH);
                        } catch (Exception ignored) {
                        }
                    }
            }

            boolean defensePressed = Gdx.input.isKeyJustPressed(Input.Keys.D);
            if (defense != null && defensePressed && !defense.isDefending() && defense.getCooldownRemaining() <= 0f) {
                defense.startDefense();
            }
        }

        if (!dash.isDashing() && !dash.isCharging() && !vel.isZero() && !lockMovement) {
            dash.setLastDirection(vel);
        }
    }

    private void updateDashState(Dash dash, Velocity velocity, float deltaTime) {
        if (!dash.isDashing()) {
            return;
        }

        float remaining = dash.getDashTimeRemaining() - deltaTime;
        if (remaining <= 0f) {
            dash.stopDash();
            velocity.getVelocity().setZero();
            return;
        }

        dash.setDashTimeRemaining(remaining);
        velocity.getVelocity().set(dash.getDashVelocity());
    }

    private void applyGravity(Velocity velocity, float deltaTime) {
        Vector2 vel = velocity.getVelocity();
        vel.y += GRAVITY * deltaTime;

        if (vel.y < MAX_FALL_SPEED) {
            vel.y = MAX_FALL_SPEED;
        }
    }

    private void updatePosition(Transform transform, Collider collider, Velocity velocity, Physics physics, float deltaTime) {
        Vector2 position = transform.getPosition();
        Vector2 vel = velocity.getVelocity();
        float hitW, hitH, offX, offY;
        if (collider != null) {
            hitW = collider.getWidth();
            hitH = collider.getHeight();
            offX = collider.getOffsetX();
            offY = collider.getOffsetY();
        } else {
            Vector2 size = transform.getSize();
            hitW = size.x;
            hitH = size.y;
            offX = 0f;
            offY = 0f;
        }

        float newX = position.x + vel.x * deltaTime;
        float newY = position.y + vel.y * deltaTime;

        Rectangle currentRect = new Rectangle(position.x + offX, position.y + offY, hitW, hitH);
        boolean dropThroughPlatforms = shouldDropThrough(currentRect, physics);

        physics.setOnGround(false);

        if (vel.y != 0) {
            float previousBottom = position.y + offY;
            if (!checkCollision(position.x + offX, newY + offY, hitW, hitH, vel.y, previousBottom, dropThroughPlatforms)) {
                position.y = newY;
            } else {
                if (vel.y < 0) {
                    physics.setOnGround(true);
                }
                vel.y = 0;
            }
        }

        if (vel.y <= 0) {
            if (checkCollision(position.x + offX, position.y + offY - 0.05f, hitW, hitH, vel.y, position.y + offY, dropThroughPlatforms)) {
                physics.setOnGround(true);
            }
        }

        if (vel.x != 0) {
            float minX = 0;
            float maxX = mapWidth - hitW;

            if (newX < minX) {
                newX = minX;
                vel.x = 0;
            } else if (newX > maxX) {
                newX = maxX;
                vel.x = 0;
            }

            if (!checkCollision(newX + offX, position.y + offY, hitW, hitH, vel.y, position.y + offY, false)) {
                position.x = newX;
            } else if (physics.isOnGround() || vel.y <= 0) {
                boolean stepped = false;
                for (float stepY = 0.05f; stepY <= STEP_HEIGHT; stepY += 0.05f) {
                    if (!checkCollision(newX + offX, position.y + offY + stepY, hitW, hitH, vel.y, position.y + offY, false)) {
                        position.x = newX;
                        position.y += stepY;
                        stepped = true;

                        physics.setOnGround(true);
                        vel.y = 0;
                        break;
                    }
                }

                if (!stepped) {
                    vel.x = 0;
                }
            } else {
                vel.x = 0;
            }
        }

        if (position.y + offY < 0) {
            position.y = -offY;
            vel.y = 0;
            physics.setOnGround(true);
        } else if (position.y + offY + hitH > mapHeight) {
            position.y = mapHeight - hitH - offY;
            vel.y = 0;
        }
    }

    private boolean checkCollision(float x, float y, float w, float h, float velocityY, float previousBottom, boolean dropThroughPlatforms) {
        Rectangle playerRect = new Rectangle(x, y, w, h);
        return collisionManager.checkCollision(playerRect, velocityY, previousBottom, dropThroughPlatforms);
    }

    
    private void triggerDash(Dash dash, Velocity velocity) {
        Vector2 vel = velocity.getVelocity();
        Vector2 inputDirection = DashController.readInputDirection();
        Vector2 dashDirection = DashController.resolveDashDirection(inputDirection, vel, dash.getLastDirection());
        dashDirection.y = 0f;
        if (dashDirection.x == 0f) {
            dashDirection.x = dash.getLastDirection().x == 0f ? 1f : Math.signum(dash.getLastDirection().x);
        }
        dashDirection.nor();
        dash.setLastDirection(dashDirection);

        int effectiveLevel = Math.max(1, Math.min(DashController.MAX_LEVEL, dash.getChargeLevel()));
        float dashSpeed = BASE_DASH_SPEED + DASH_SPEED_PER_LEVEL * (effectiveLevel - 1);
        Vector2 dashVelocity = new Vector2(dashDirection).scl(dashSpeed);

        dash.startDash(dashVelocity, DASH_DURATION);
        vel.set(dashVelocity);
        dash.setCharging(false);
        dash.setChargeTime(0f);
        dash.setChargeLevel(1);
        dash.setCooldownRemaining(DASH_COOLDOWN);
        dash.setHoldTime(0f);
    }

    private boolean shouldDropThrough(Rectangle playerRect, Physics physics) {
        if (dropThroughTimer > 0f) {
            if (physics != null) {
                physics.setOnGround(false);
            }
            return true;
        }

        if (physics == null || !physics.isOnGround()) {
            return false;
        }
        if (!isDropThroughInputActive()) {
            return false;
        }
        if (!isPlayerInsideUtilityLayer(playerRect)) {
            return false;
        }

        dropThroughTimer = DROP_THROUGH_GRACE;
        physics.setOnGround(false);
        return true;
    }

    private void updateDropThroughTimer(float deltaTime) {
        if (dropThroughTimer <= 0f) {
            return;
        }
        dropThroughTimer = Math.max(0f, dropThroughTimer - deltaTime);
    }

    private boolean isDropThroughInputActive() {
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
        return ctrl && down;
    }

    private boolean isPlayerInsideUtilityLayer(Rectangle playerRect) {
        if (currentMap == null || playerRect == null) {
            return false;
        }

        for (MapLayer layer : currentMap.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer tileLayer)) {
                continue;
            }

            if (!isUtilityLayer(layer)) {
                continue;
            }

            if (overlapsUtilityCell(tileLayer, playerRect)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPlayerInsideNamedLayer(Rectangle playerRect, String layerName) {
        if (currentMap == null || playerRect == null || layerName == null) return false;

        String normalizedTarget = layerName.trim().toUpperCase(Locale.ROOT);

        for (MapLayer layer : currentMap.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer tileLayer)) continue;

            if (!normalizedTarget.equals(layer.getName() == null ? "" : layer.getName().trim().toUpperCase(Locale.ROOT))) continue;

            if (overlapsUtilityCell(tileLayer, playerRect)) {
                return true;
            }
        }

        return false;
    }

    private boolean overlapsUtilityCell(TiledMapTileLayer tileLayer, Rectangle playerRect) {
        float tileWidth = tileLayer.getTileWidth() * Lynguard.UNIT_SCALE;
        float tileHeight = tileLayer.getTileHeight() * Lynguard.UNIT_SCALE;

        int leftTile = Math.max(0, (int) (playerRect.x / tileWidth));
        int rightTile = Math.min(tileLayer.getWidth() - 1, (int) ((playerRect.x + playerRect.width) / tileWidth));
        int bottomTile = Math.max(0, (int) (playerRect.y / tileHeight));
        int topTile = Math.min(tileLayer.getHeight() - 1, (int) ((playerRect.y + playerRect.height) / tileHeight));

        for (int row = bottomTile; row <= topTile; row++) {
            for (int col = leftTile; col <= rightTile; col++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(col, row);
                if (cell != null && cell.getTile() != null) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isUtilityLayer(MapLayer layer) {
        if (layer == null || layer.getName() == null) {
            return false;
        }
        String normalized = layer.getName().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("UTILITY_LAYER")) {
            return false;
        }
        return layer.getProperties().containsKey("pass_crouch_zone");
    }

    public boolean canDropThrough(Entity player) {
        Physics physics = Physics.MAPPER.get(player);
        Transform transform = Transform.MAPPER.get(player);
        Collider collider = Collider.MAPPER.get(player);
        
        if (physics == null || !physics.isOnGround() || transform == null) {
            return false;
        }

        Vector2 position = transform.getPosition();
        float hitW = transform.getSize().x * transform.getScaling().x;
        float hitH = transform.getSize().y * transform.getScaling().y;
        float offX = 0;
        float offY = 0;

        if (collider != null) {
            hitW = collider.getWidth();
            hitH = collider.getHeight();
            offX = collider.getOffsetX();
            offY = collider.getOffsetY();
        }

        Rectangle playerRect = new Rectangle(position.x + offX, position.y + offY, hitW, hitH);
        return isPlayerInsideUtilityLayer(playerRect);
    }
}
