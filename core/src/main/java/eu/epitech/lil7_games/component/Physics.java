package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Physics implements Component {
    public static final ComponentMapper<Physics> MAPPER = ComponentMapper.getFor(Physics.class);

    private boolean onGround;
    private boolean canJump;

    public Physics() {
        this.onGround = false;
        this.canJump = false;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean canJump() {
        return canJump;
    }

    public void setCanJump(boolean canJump) {
        this.canJump = canJump;
    }
}
