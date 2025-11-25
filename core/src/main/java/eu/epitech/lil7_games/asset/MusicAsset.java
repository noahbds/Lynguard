package eu.epitech.lil7_games.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.audio.Music;

public enum MusicAsset implements Asset<Music> {
    TOWN("FIlaments.ogg"),
    MAP0("Views.ogg"),

    MAP1(""),
    MAP2("");


    private final AssetDescriptor<Music> descriptor;

    MusicAsset(String musicFile) {
        this.descriptor = new AssetDescriptor<>("music/" + musicFile, Music.class);
    }

    @Override
    public AssetDescriptor<Music> getDescriptor() {
        return descriptor;
    }
}
