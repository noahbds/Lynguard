package eu.epitech.lil7_games.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.component.Player;
import eu.epitech.lil7_games.component.Transform;
import eu.epitech.lil7_games.ui.model.HudViewModel;

public class InteractionSystem extends IteratingSystem {
    private TiledMap currentMap;
    private final HudViewModel hudViewModel;
    private final LevelTransitionSystem levelTransitionSystem;

    public InteractionSystem(HudViewModel hudViewModel, LevelTransitionSystem levelTransitionSystem) {
        super(Family.all(Player.class, Transform.class).get());
        this.hudViewModel = hudViewModel;
        this.levelTransitionSystem = levelTransitionSystem;
    }

    public void setMap(TiledMap map) {
        this.currentMap = map;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (currentMap == null) return;

        Transform transform = Transform.MAPPER.get(entity);
        Player player = Player.MAPPER.get(entity);
        Vector2 position = transform.getPosition();
        Vector2 size = transform.getSize();
        Rectangle playerBounds = new Rectangle(position.x, position.y, size.x, size.y);

        boolean actionPossible = false;
        String actionText = "";

        for (MapLayer layer : currentMap.getLayers()) {
            // Handle Tile Layers (Doors, etc.)
            if (layer instanceof TiledMapTileLayer tileLayer) {
                if (isPlayerOnActiveCell(tileLayer, playerBounds)) {
                    MapProperties props = layer.getProperties();
                    if (processLayerProperties(props, player, actionPossible)) {
                        actionPossible = true;
                        actionText = getActionText(props, player);
                    }
                }
            } 
            // Handle Object Layers (Chests, etc.)
            else if (layer.getObjects().getCount() > 0) {
                for (com.badlogic.gdx.maps.MapObject object : layer.getObjects()) {
                    if (isPlayerOverlappingObject(object, playerBounds)) {
                        // Check properties on the Layer first (as per user screenshot)
                        MapProperties layerProps = layer.getProperties();
                        if (processLayerProperties(layerProps, player, actionPossible)) {
                            actionPossible = true;
                            actionText = getActionText(layerProps, player);
                        }
                        // Also check properties on the Object itself (override or addition)
                        MapProperties objectProps = object.getProperties();
                        if (processLayerProperties(objectProps, player, actionPossible)) {
                            actionPossible = true;
                            actionText = getActionText(objectProps, player);
                        }
                    }
                }
            }
        }
        
        hudViewModel.setActionPossible(actionPossible);
        hudViewModel.setActionText(actionText);
    }

    private boolean processLayerProperties(MapProperties props, Player player, boolean currentActionPossible) {
        boolean actionFound = false;

        // Chest Logic
        if (props.containsKey("KEY1") || props.containsKey("KEY2")) {
             String keyToGive = null;
             if (props.containsKey("KEY1")) keyToGive = "KEY1";
             else if (props.containsKey("KEY2")) keyToGive = "KEY2";
             
             if (keyToGive != null && !player.keys.contains(keyToGive)) {
                 actionFound = true;
                 if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                     // give the key, update HUD and remove the property so chest can't be looted again
                     player.keys.add(keyToGive);
                     hudViewModel.addKey(keyToGive);
                     try {
                         props.remove(keyToGive);
                     } catch (Exception ex) {
                         Gdx.app.error("InteractionSystem", "Failed to remove key property from chest/object: " + ex.getMessage(), ex);
                     }
                     // debug: key pickup log removed
                 }
             }
        }
        
        // Door Logic
        if (props.containsKey("isclosed")) {
            String isClosed = props.get("isclosed", String.class);
            if ("TRUE".equals(isClosed)) {
                String needKey1 = props.get("NEEDKEY1", String.class);
                String needKey2 = props.get("NEEDKEY2", String.class);
                
                boolean hasKey1 = needKey1 == null || !"TRUE".equals(needKey1) || player.keys.contains("KEY1");
                boolean hasKey2 = needKey2 == null || !"TRUE".equals(needKey2) || player.keys.contains("KEY2");
                
                actionFound = true; // Always show status for door
                
                if (hasKey1 && hasKey2) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                        // consume the required keys from player & HUD
                        if ("TRUE".equals(needKey1)) {
                            player.keys.remove("KEY1");
                            hudViewModel.removeKey("KEY1");
                            // debug: consumed KEY1 log removed
                        }
                        if ("TRUE".equals(needKey2)) {
                            player.keys.remove("KEY2");
                            hudViewModel.removeKey("KEY2");
                            // debug: consumed KEY2 log removed
                        }

                        // mark the door as opened so the tile/object is no longer closed
                        try {
                            props.put("isclosed", "FALSE");
                        } catch (Exception ex) {
                            Gdx.app.error("InteractionSystem", "Failed to set isclosed=FALSE on door: " + ex.getMessage(), ex);
                        }

                        // trigger the transition defined on this props set (layer or object)
                        java.util.Iterator<String> keys = props.getKeys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            if (key.startsWith("level_transition_")) {
                                // persist unlocked state so this door remains open when reloading the map
                                levelTransitionSystem.markTransitionUnlocked(key);
                                levelTransitionSystem.triggerTransitionFromProperty(key);
                            }
                        }
                    }
                }
            }
        }
        return actionFound;
    }

    private String getActionText(MapProperties props, Player player) {
        if (props.containsKey("KEY1") || props.containsKey("KEY2")) {
             String keyToGive = null;
             if (props.containsKey("KEY1")) keyToGive = "KEY1";
             else if (props.containsKey("KEY2")) keyToGive = "KEY2";
             if (keyToGive != null && !player.keys.contains(keyToGive)) {
                 return "Open Chest";
             }
        }
        
        if (props.containsKey("isclosed")) {
            String isClosed = props.get("isclosed", String.class);
            if ("TRUE".equals(isClosed)) {
                String needKey1 = props.get("NEEDKEY1", String.class);
                String needKey2 = props.get("NEEDKEY2", String.class);
                
                boolean hasKey1 = needKey1 == null || !"TRUE".equals(needKey1) || player.keys.contains("KEY1");
                boolean hasKey2 = needKey2 == null || !"TRUE".equals(needKey2) || player.keys.contains("KEY2");
                
                if (hasKey1 && hasKey2) {
                    return "Unlock Door";
                } else {
                    return "Locked (Need Keys)";
                }
            }
        }
        return "";
    }

    private boolean isPlayerOverlappingObject(com.badlogic.gdx.maps.MapObject object, Rectangle playerBounds) {
        Rectangle objectRect = null;

        if (object instanceof com.badlogic.gdx.maps.objects.RectangleMapObject rectObj) {
            objectRect = rectObj.getRectangle();
        } else if (object instanceof com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject tileObj) {
            float x = tileObj.getX();
            float y = tileObj.getY();
            float width = 0;
            float height = 0;
            
            if (tileObj.getProperties().containsKey("width")) {
                width = tileObj.getProperties().get("width", Float.class);
            } else if (tileObj.getTile() != null) {
                width = tileObj.getTile().getTextureRegion().getRegionWidth();
            }
            
            if (tileObj.getProperties().containsKey("height")) {
                height = tileObj.getProperties().get("height", Float.class);
            } else if (tileObj.getTile() != null) {
                height = tileObj.getTile().getTextureRegion().getRegionHeight();
            }
            
            objectRect = new Rectangle(x, y, width, height);
        }

        if (objectRect != null) {
            Rectangle scaledRect = new Rectangle(
                objectRect.x * Lynguard.UNIT_SCALE, 
                objectRect.y * Lynguard.UNIT_SCALE, 
                objectRect.width * Lynguard.UNIT_SCALE, 
                objectRect.height * Lynguard.UNIT_SCALE
            );
            
            // debug: object overlap log removed
            
            return playerBounds.overlaps(scaledRect);
        }
        return false;
    }

    private boolean isPlayerOnActiveCell(TiledMapTileLayer tileLayer, Rectangle playerBounds) {
        float tileWidth = tileLayer.getTileWidth() * Lynguard.UNIT_SCALE;
        float tileHeight = tileLayer.getTileHeight() * Lynguard.UNIT_SCALE;
        int leftTile = Math.max(0, (int) (playerBounds.x / tileWidth));
        int rightTile = Math.min(tileLayer.getWidth() - 1, (int) ((playerBounds.x + playerBounds.width) / tileWidth));
        int bottomTile = Math.max(0, (int) (playerBounds.y / tileHeight));
        int topTile = Math.min(tileLayer.getHeight() - 1, (int) ((playerBounds.y + playerBounds.height) / tileHeight));

        Rectangle cellBounds = new Rectangle(0f, 0f, tileWidth, tileHeight);
        for (int y = bottomTile; y <= topTile; y++) {
            for (int x = leftTile; x <= rightTile; x++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell == null || cell.getTile() == null) {
                    continue;
                }

                cellBounds.setPosition(x * tileWidth, y * tileHeight);
                if (playerBounds.overlaps(cellBounds)) {
                    return true;
                }
            }
        }
        return false;
    }
}
