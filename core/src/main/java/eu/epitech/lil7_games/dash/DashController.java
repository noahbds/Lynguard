package eu.epitech.lil7_games.dash;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

public final class DashController {
    public static final int MAX_LEVEL = 5;

    private DashController() {
    }

    public static int computeChargeLevel(float chargeTime, float maxChargeTime) {
        if (maxChargeTime <= 0f) {
            return 1;
        }
        float ratio = Math.max(0f, Math.min(1f, chargeTime / maxChargeTime));
        int level = (int) (ratio * MAX_LEVEL) + 1;
        return Math.min(level, MAX_LEVEL);
    }

    public static Vector2 readInputDirection() {
        float x = 0f;
        float y = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            x += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.Q)) {
            x -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.Z)) {
            y += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            y -= 1f;
        }

        Vector2 direction = new Vector2(x, y);
        if (!direction.isZero()) {
            direction.nor();
        }
        return direction;
    }

    public static Vector2 resolveDashDirection(Vector2 inputDirection, Vector2 velocity, Vector2 fallbackDirection) {
        if (inputDirection != null && !inputDirection.isZero()) {
            return new Vector2(inputDirection).nor();
        }

        if (velocity != null && !velocity.isZero()) {
            return new Vector2(velocity).nor();
        }

        if (fallbackDirection != null && !fallbackDirection.isZero()) {
            return new Vector2(fallbackDirection).nor();
        }

        return new Vector2(1f, 0f);
    }
}
