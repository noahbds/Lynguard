package eu.epitech.lil7_games.system;

import java.util.Comparator;
import java.util.Locale;

import eu.epitech.lil7_games.component.Transform;

import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.component.Flip;
import eu.epitech.lil7_games.component.Graphic;

public class RenderSystem extends SortedIteratingSystem implements Disposable {
    /*
     * System de rendu :
     * 1. Trie les entités par Transform (z) pour garantir l'ordre.
     * 2. Rend les layers Tiled intercalés avec les entités quand leur z dépasse l'index courant.
     * Note : On pourrait optimiser en pré-calculant la séquence (layers vs entités) si un coût per-frame
     * apparaît, mais pour un nombre de layers modéré c'est acceptable.
     */
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final Batch batch;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private TiledMap currentMap;
    private int currentLayerIndex = 0;
    private int totalLayers = 0;
    // Debug renderer removed after cleanup; repacking assets should fix baseline issues.

    public RenderSystem(Batch batch, Viewport viewport, OrthographicCamera camera) {
        super(Family.all(Transform.class, Graphic.class).get(),
            Comparator.comparing(eu.epitech.lil7_games.component.Transform.MAPPER::get)
        );
        this.batch = batch;
        this.viewport = viewport;
        this.camera = camera;
    this.mapRenderer = new OrthogonalTiledMapRenderer(null, Lynguard.UNIT_SCALE, this.batch );
    }

    @Override
    public void update(float deltaTime) {
        this.viewport.apply();
        this.batch.setColor(Color.WHITE);
        this.mapRenderer.setView(this.camera);
        
        currentLayerIndex = 0;
        totalLayers = (currentMap != null) ? currentMap.getLayers().getCount() : 0;
        
        forceSort();
        batch.begin();
        batch.setProjectionMatrix(camera.combined);
        super.update(deltaTime);
        
        while (currentLayerIndex < totalLayers) {
            renderLayer(currentLayerIndex);
            currentLayerIndex++;
        }
        
        batch.end();
    }
    
    private void renderLayer(int layerIndex) {
        if (currentMap == null || layerIndex >= totalLayers) return;
        
        MapLayer layer = currentMap.getLayers().get(layerIndex);
        
        // Skip rendering for special gameplay-only layers
        String layerName = layer.getName();
        if (shouldSkipLayer(layerName)) {
            return;
        }
        
        if (layer instanceof TiledMapTileLayer) {
            batch.end();
            
            int[] layers = new int[]{layerIndex};
            mapRenderer.render(layers);
            
            batch.begin();
            batch.setProjectionMatrix(camera.combined);
        }
    }

    @Override
    protected void processEntity(com.badlogic.ashley.core.Entity entity, float deltaTime) {
        Transform transform = Transform.MAPPER.get(entity);
        Graphic graphic = Graphic.MAPPER.get(entity);
        
        if(graphic.getRegion() == null) {
            return;
        }
        
        int entityZ = transform.getZ();
        while (currentLayerIndex < entityZ && currentLayerIndex < totalLayers) {
            renderLayer(currentLayerIndex);
            currentLayerIndex++;
        }
        
    Vector2 position = transform.getPosition();
        Vector2 scaling = transform.getScaling();
        Vector2 size = transform.getSize();
        
        Flip flip = Flip.MAPPER.get(entity);
        float scaleX = scaling.x;
        float scaleY = scaling.y;
        if (flip != null) {
            if (flip.isFlipX()) scaleX = -scaleX;
            if (flip.isFlipY()) scaleY = -scaleY;
        }
        
        this.batch.setColor(graphic.getColor());
        // Calcul de baseX/baseY pour recentrer correctement en cas de flip ou scale.
        float baseX = position.x - (1f - Math.abs(scaleX)) * size.x * 0.5f;
        float baseY = position.y - (1f - Math.abs(scaleY)) * size.y * 0.5f;
        this.batch.draw(
            graphic.getRegion(),
            baseX,
            baseY,
            size.x * 0.5f, size.y * 0.5f,
            size.x, size.y,
            scaleX, scaleY,
            transform.getRotationDeg()
        );
        this.batch.setColor(Color.WHITE);

    }

    //@Override
    //protected void processEntity(Entity entity, float deltatime){
    //}


    public void setMap(TiledMap tileMap){
        this.mapRenderer.setMap(tileMap);
        this.currentMap = tileMap;
    }

    private boolean shouldSkipLayer(String layerName) {
        if (layerName == null) {
            return false;
        }

        String normalized = layerName.toUpperCase(Locale.ROOT);
        return normalized.startsWith("PLAYER_SPAWN") ||
            normalized.equals("INV_BAR") ||
            normalized.equals("JUMP_BOOST") ||
            normalized.startsWith("LEVEL_TRANSITION") ||
            normalized.startsWith("LEVEL_RETURN") ||
            normalized.startsWith("RETURN_LEVEL") ||
            normalized.startsWith("SLIMES") ||
            normalized.startsWith("UTILITY_LAYER") ||
            normalized.startsWith("SPIKES_DMG");
    }

    @Override
    public void dispose() {
        this.mapRenderer.dispose();
    }
}

