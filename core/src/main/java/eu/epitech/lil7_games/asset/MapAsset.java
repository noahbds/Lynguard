package eu.epitech.lil7_games.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.BaseTiledMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;

public enum MapAsset implements Asset<TiledMap> {
    // LEVEL 1 to 15 (excluding lEVEL 11)
    MAIN("LIL7_Level_0.tmx"),
    LEVEL_0("LIL7_Level_0.tmx"),
    LEVEL_1("LIL7_Level_1.tmx"),
    LEVEL_2("LIL7_Level_2.tmx"),
    LEVEL_3("LIL7_Level_3.tmx"),
    LEVEL_4("LIL7_Level_4.tmx"),
    LEVEL_5("LIL7_Level_5.tmx"),
    LEVEL_6("LIL7_Level_6.tmx"),
    LEVEL_7("LIL7_Level_7.tmx"),
    LEVEL_8("LIL7_Level_8.tmx"),
    LEVEL_9("LIL7_Level_9.tmx"),
    LEVEL_10("LIL7_Level_10.tmx"),
    LEVEL_12("LIL7_Level_12.tmx"),
    LEVEL_13("LIL7_Level_13.tmx"),
    LEVEL_14("LIL7_Level_14.tmx"),
    LEVEL_15("LIL7_Level_15.tmx");


    private final AssetDescriptor<TiledMap> descriptor;

    MapAsset(String mapName) {
        BaseTiledMapLoader.Parameters parameters = new BaseTiledMapLoader.Parameters();
        parameters.generateMipMaps = true;
        this.descriptor = new AssetDescriptor<>("maps/" + mapName, TiledMap.class, parameters);
    }

    @Override
    public AssetDescriptor<TiledMap> getDescriptor() {
        return this.descriptor;
    }
}
