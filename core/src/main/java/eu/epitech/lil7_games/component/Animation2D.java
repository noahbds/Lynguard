package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import eu.epitech.lil7_games.asset.AtlasAsset;
import eu.epitech.lil7_games.component.Facing.FacingDirection;

public class Animation2D implements Component {
    public static final ComponentMapper<Animation2D> MAPPER = ComponentMapper.getFor(Animation2D.class);

    private final AtlasAsset atlasAsset;
    private final String atlasKey;
    private AnimationType type;
    private FacingDirection direction;
    private PlayMode playMode;
    private float speed;
    private float stateTime;
    private Animation<TextureRegion> animation;
    private boolean dirty;
    private int attackComboStage = 1; // used when type == ATTACK

    public Animation2D(AtlasAsset atlasAsset, String atlasKey, AnimationType type, PlayMode playMode, float speed) {
        this.atlasAsset = atlasAsset;
        this.atlasKey = atlasKey;
        this.type = type;
        this.playMode = playMode;
        this.speed = speed;
        this.stateTime = 0f;
        this.animation = null;
        this.dirty = true;
    }

    public void setAnimation(Animation<TextureRegion> animation, FacingDirection direction) {
        this.animation = animation;
        this.stateTime = 0f;
        this.direction = direction;
        this.dirty = false;
    }

    public FacingDirection getDirection() {
        return direction;
    }

    public Animation<TextureRegion> getAnimation() {
        return animation;
    }

    public AtlasAsset getAtlasAsset() {
        return atlasAsset;
    }

    public String getAtlasKey() {
        return atlasKey;
    }

    public void setType(AnimationType type) {
        this.type = type;
        this.dirty = true;
    }

    public AnimationType getType() {
        return type;
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
    }

    public float getSpeed() { return speed; }

    public boolean isDirty() {
        return dirty;
    }

    public int getAttackComboStage() { return attackComboStage; }
    public void setAttackComboStage(int stage) {
        if (stage < 1) stage = 1; else if (stage > 3) stage = 3;
        if (this.attackComboStage != stage) {
            this.attackComboStage = stage;
            if (this.type == AnimationType.ATTACK) {
                this.dirty = true; // force rebuild with new subset
            }
        }
    }

    public float incAndGetStateTime(float deltaTime) {
        this.stateTime += deltaTime * speed;
        return this.stateTime;
    }

    public float getStateTime() {
        return this.stateTime;
    }

    public boolean isFinished() {
        return animation.isAnimationFinished(stateTime);
    }

    public enum AnimationType {
        IDLE("idle"),
        WALK("walk"),
        RUN("run"),
        JUMP("jump"),
        ATTACK("attack"),
        DEFEND("defend"),
        DEATH("death"),
        ;

        private final String atlasKey;

        AnimationType(String atlasKey) {
            this.atlasKey = atlasKey;
        }

        public String getAtlasKey() {
            return atlasKey;
        }
    }
}
