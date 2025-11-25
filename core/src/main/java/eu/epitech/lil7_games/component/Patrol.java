package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Patrol implements Component {
    public static final ComponentMapper<Patrol> MAPPER = ComponentMapper.getFor(Patrol.class);

    private float leftBound;
    private float rightBound;
    private float speed;
    private boolean movingRight;

    public Patrol(float leftBound, float rightBound, float speed, boolean startMovingRight) {
        this.leftBound = Math.min(leftBound, rightBound);
        this.rightBound = Math.max(leftBound, rightBound);
        this.speed = speed;
        this.movingRight = startMovingRight;
    }

    public float getLeftBound() {
        return leftBound;
    }

    public void setLeftBound(float leftBound) {
        this.leftBound = leftBound;
    }

    public float getRightBound() {
        return rightBound;
    }

    public void setRightBound(float rightBound) {
        this.rightBound = rightBound;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isMovingRight() {
        return movingRight;
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }
}
