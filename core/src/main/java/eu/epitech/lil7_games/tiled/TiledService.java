package eu.epitech.lil7_games.tiled;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.utils.GdxRuntimeException;

import eu.epitech.lil7_games.asset.AssetService;
import eu.epitech.lil7_games.asset.MapAsset;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TiledService {
    private final AssetService assetService;

    private TiledMap currentMap;

    private Consumer<TiledMap> mapChangeConsumer;
    private BiConsumer<TiledMapTileMapObject, Integer> loadObjectConsumer;

    public TiledService(AssetService assetService) {
        this.assetService = assetService;
        this.mapChangeConsumer = null;
        this.loadObjectConsumer = null;
        this.currentMap = null;
    }

    public TiledMap loadMap(MapAsset mapAsset){
        TiledMap tiledMap = this.assetService.load(mapAsset);
        tiledMap.getProperties().put("mapAsset", mapAsset);
        return tiledMap;
    }

    public void setMap(TiledMap map){
        if (this.currentMap != null){
            this.assetService.unload(this.currentMap.getProperties().get("mapAsset", MapAsset.class));
        }

        this.currentMap = map;
        loadMapObjets(map);
        if (this.mapChangeConsumer != null) {
            this.mapChangeConsumer.accept(map);
        }
    }

    private void loadMapObjets(TiledMap tiledMap){
        int layerIndex = 0;
        for (MapLayer layer: tiledMap.getLayers()){
            if (layer.getObjects().getCount() > 0) {
                loadObjectLayer(layer, layerIndex);
            }
            layerIndex++;
        }

    }

    private void loadObjectLayer(MapLayer objectLayer, int layerIndex){
        if (loadObjectConsumer == null) return;

        for (MapObject mapObject : objectLayer.getObjects()){
            if (mapObject instanceof TiledMapTileMapObject tileMapObject){
                loadObjectConsumer.accept(tileMapObject, layerIndex);
            } else {
                throw new GdxRuntimeException("unsupported object: ");
            }
        }
    }

    public void setMapChangeConsumer(Consumer<TiledMap> mapChangeConsumer){
        this.mapChangeConsumer = mapChangeConsumer;
    }
    public void setLoadObjectConsumer(BiConsumer<TiledMapTileMapObject, Integer> loadObjectConsumer){
        this.loadObjectConsumer = loadObjectConsumer;
    }
}
