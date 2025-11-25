package eu.epitech.lil7_games.tiled;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Vector2;

import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.AssetService;
import eu.epitech.lil7_games.asset.AtlasAsset;
import eu.epitech.lil7_games.component.AnimatedTile;
import eu.epitech.lil7_games.component.Animation2D;
import eu.epitech.lil7_games.component.Facing;
import eu.epitech.lil7_games.component.Flip;
import eu.epitech.lil7_games.component.Graphic;
import eu.epitech.lil7_games.component.Transform;

public class TiledAshleyConfigurator {
    private final Engine engine;
    private final AssetService assetService;

    public TiledAshleyConfigurator(Engine engine, AssetService assetService) {
        this.engine = engine;
        this.assetService = assetService;
    }

    public void onLoadObject(TiledMapTileMapObject tileMapObject, int layerIndex) {
        Entity entity = this.engine.createEntity();
        TiledMapTile tile = tileMapObject.getTile();
        TextureRegion textureRegion = getTextureRegion(tile);
        Integer zValue = tile.getProperties().get("z", Integer.class);
        int z = (zValue != null) ? zValue : layerIndex;

        entity.add(new Graphic(Color.WHITE.cpy(), textureRegion));
        entity.add(new AnimatedTile(tile));
        entity.add(new Flip(tileMapObject.isFlipHorizontally(), tileMapObject.isFlipVertically()));

        Facing.FacingDirection facingDir = tileMapObject.isFlipHorizontally() ? Facing.FacingDirection.RIGHT : Facing.FacingDirection.LEFT;
        entity.add(new Facing(facingDir));

        addEntityTransform(
            tileMapObject.getX(), tileMapObject.getY(), z,
            textureRegion.getRegionWidth(), textureRegion.getRegionHeight(),
            tileMapObject.getScaleX(), tileMapObject.getScaleY(),
            entity);

        addEntityAnimation(tile, entity);

        this.engine.addEntity(entity);

        // audio emitters are created centrally in AudioService.setMap
    }

    private void addEntityAnimation(TiledMapTile tile, Entity entity) {
        String animationStr = tile.getProperties().get("animation", String.class);
        if (animationStr == null || animationStr.isBlank()) return;

        Animation2D.AnimationType animationType;
        try {
            animationType = Animation2D.AnimationType.valueOf(animationStr.trim().toUpperCase());
        } catch (Exception e) {
            return;
        }

        String atlasAssetStr = tile.getProperties().get("atlasAsset", "OBJECTS", String.class);
        AtlasAsset atlasAsset;
        try {
            atlasAsset = AtlasAsset.valueOf(atlasAssetStr.trim().toUpperCase());
        } catch (Exception e) {
            atlasAsset = AtlasAsset.OBJECTS;
        }

        FileTextureData textureData = (FileTextureData) tile.getTextureRegion().getTexture().getTextureData();
        String atlasKey = textureData.getFileHandle().nameWithoutExtension();
        Float speedObj = tile.getProperties().get("animationSpeed", Float.class);
        float speed = (speedObj != null && speedObj > 0f) ? speedObj : 0.1f;

        entity.add(new Animation2D(atlasAsset, atlasKey, animationType, Animation.PlayMode.LOOP, speed));
    }



    private static void addEntityTransform(
        float x, float y, int z,
        float w, float h,
        float scaleX, float scaleY,
        Entity entity
    ) {
        Vector2 position = new Vector2(x, y);
        Vector2 size = new Vector2(w, h);
        Vector2 scaling = new Vector2(scaleX, scaleY);

        position.scl(Lynguard.UNIT_SCALE);
        size.scl(Lynguard.UNIT_SCALE);

        entity.add(new Transform(position, z, size, scaling, 0f));
    }

    private TextureRegion getTextureRegion(TiledMapTile tile) {
        return tile.getTextureRegion();
    }
}
