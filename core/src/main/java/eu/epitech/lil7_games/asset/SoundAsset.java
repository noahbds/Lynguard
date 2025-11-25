package eu.epitech.lil7_games.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.audio.Sound;

public enum SoundAsset implements Asset<Sound> {
    WATERFALL("waterfall.ogg"),
    BIRDS("birds.ogg"),
    SWORDSLASH("swordslash.ogg"),
    JUMP("jump.ogg");

    private final AssetDescriptor<Sound> descriptor;

    SoundAsset(String filename) {
        this.descriptor = new AssetDescriptor<>("sound/" + filename, Sound.class);
    }

    @Override
    public AssetDescriptor<Sound> getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return name();
    }
}
