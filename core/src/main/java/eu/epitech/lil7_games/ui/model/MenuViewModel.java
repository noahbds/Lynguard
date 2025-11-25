package eu.epitech.lil7_games.ui.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import eu.epitech.lil7_games.GameScreen;
import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.MapAsset;
import eu.epitech.lil7_games.audio.AudioService;
import eu.epitech.lil7_games.screen.MenuScreen;
import eu.epitech.lil7_games.save.SaveData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class MenuViewModel extends ViewModel {
    public static final String GAME_RUNNING = "gameRunning";
    private static final String SAVE_FOLDER = "saves";
    private static final String QUICK_SAVE_SLOT = "Quick Save";
    private static final String AUTO_SAVE_SLOT = "Auto Save";

    private final AudioService audioService;
    private final Array<String> saveSlots = new Array<>();
    private final ObjectMap<String, String> saveDetails = new ObjectMap<>();
    private final ObjectMap<String, Long> saveTimestamps = new ObjectMap<>();
    private final FileHandle saveDirectory;
    private final Json json;
    private final DateFormat dateFormat;
    private boolean gameRunning;

    public MenuViewModel(Lynguard game) {
        super(game);
        this.audioService = game.getAudioService();
        this.gameRunning = game.isGameRunning();
        this.saveDirectory = Gdx.files.local(SAVE_FOLDER);
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        this.json = new Json();
        this.dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());
        loadExistingSaves();
    }

    public float getMusicVolume() {
        return audioService.getMusicVolume();
    }

    public float getSoundVolume() {
        return audioService.getSoundVolume();
    }

    public void setMusicVolume(float volume) {
        audioService.setMusicVolume(volume);
    }

    public void setSoundVolume(float volume) {
        audioService.setSoundVolume(volume);
    }

    public void startGame() {
        activateGameSession();
    }

    public void continueGame() {
        String latestSlot = getLatestSlotName();
        if (latestSlot != null) {
            loadSlot(latestSlot);
        } else {
            activateGameSession();
        }
    }

    public void resumeGame() {
        activateGameSession();
    }

    public void openSettings() {
        Gdx.app.log("Menu", "Settings menu requested");
    }

    public void quickSave() {
        saveToSlot(QUICK_SAVE_SLOT);
    }

    public void quickLoad() {
        if (hasQuickSave()) {
            loadSlot(QUICK_SAVE_SLOT);
        }
    }

    public void autoSave() {
        saveToSlot(AUTO_SAVE_SLOT);
    }

    public void loadLatestSave() {
        String latest = getLatestSlotName();
        if (latest != null) {
            loadSlot(latest);
        }
    }

    public void saveToSlot(String slotName) {
        if (slotName == null) {
            return;
        }
        String trimmed = slotName.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        long timestamp = TimeUtils.millis();
        SaveData data = writeSlot(trimmed, timestamp);
        if (data != null) {
            registerSlot(data);
            Gdx.app.log("Menu", "Saved slot: " + trimmed);
        }
    }

    public void loadSlot(String slotName) {
        if (slotName == null || slotName.isEmpty()) {
            return;
        }
        if (!saveSlots.contains(slotName, false)) {
            Gdx.app.log("Menu", "Cannot load missing slot: " + slotName);
            return;
        }
        Gdx.app.log("Menu", "Load slot: " + slotName);
        
        FileHandle file = slotFile(slotName);
        try {
            SaveData data = json.fromJson(SaveData.class, file);
            
            GameScreen gameScreen = game.getScreen(GameScreen.class);
            if (gameScreen == null) {
                gameScreen = new GameScreen(game);
                game.addScreen(gameScreen);
            }
            
            gameScreen.setSaveData(data);
            activateGameSession();
            
        } catch (Exception e) {
            Gdx.app.error("Menu", "Failed to load save data", e);
        }
    }

    public void deleteSlot(String slotName) {
        if (slotName == null || slotName.isEmpty()) {
            return;
        }
        if (saveSlots.removeValue(slotName, false)) {
            saveDetails.remove(slotName);
            saveTimestamps.remove(slotName);
            FileHandle handle = slotFile(slotName);
            if (handle.exists()) {
                handle.delete();
            }
            Gdx.app.log("Menu", "Deleted slot: " + slotName);
        }
    }

    public Array<String> getSaveSlots() {
        return new Array<>(saveSlots);
    }

    public String describeSlot(String slotName) {
        if (slotName == null) {
            return "Select a slot to view details.";
        }
        return saveDetails.get(slotName, "Empty slot");
    }

    public void returnToMainMenu() {
        Gdx.app.log("Menu", "Return to main menu requested");
        setGameRunning(false);
        game.setScreen(MenuScreen.class);
    }

    public void goToMainMenu() {
        Gdx.app.log("Menu", "Go to main menu requested");
        setGameRunning(false);
        game.setScreen(MenuScreen.class);
    }

    public void restartGame() {
        Gdx.app.log("Menu", "Restart game requested");
        
        GameScreen currentGameScreen = game.getScreen(GameScreen.class);
        java.util.Map<String, java.util.Set<String>> deadEnemies = null;
        String currentLevelName = null;
        
        if (currentGameScreen != null) {
            deadEnemies = currentGameScreen.getDeadEnemiesPerLevel();
            MapAsset currentLevel = currentGameScreen.getCurrentLevel();
            if (currentLevel != null) {
                currentLevelName = currentLevel.name();
            }
        }
        
        GameScreen newGameScreen = new GameScreen(game);
        
        if (deadEnemies != null && !deadEnemies.isEmpty()) {
            SaveData restartData = new SaveData();
            restartData.deadEnemies = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, java.util.Set<String>> entry : deadEnemies.entrySet()) {
                restartData.deadEnemies.put(entry.getKey(), new java.util.ArrayList<>(entry.getValue()));
            }
            restartData.levelName = currentLevelName;
            newGameScreen.setSaveData(restartData);
        }
        
        game.addScreen(newGameScreen);
        activateGameSession();
    }

    public String getLatestSaveSlot() {
        return saveSlots.size == 0 ? null : saveSlots.first();
    }

    public void quitGame() {
        setGameRunning(false);
        Gdx.app.exit();
    }

    public boolean hasSaveData() {
        return saveSlots.size > 0;
    }

    public boolean hasQuickSave() {
        return saveSlots.contains(QUICK_SAVE_SLOT, false);
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public String getLatestSlotName() {
        return saveSlots.size == 0 ? null : saveSlots.first();
    }

    private void activateGameSession() {
        setGameRunning(true);
        game.setScreen(GameScreen.class);
    }

    private void setGameRunning(boolean running) {
        if (this.gameRunning != running) {
            this.propertyChangeSupport.firePropertyChange(GAME_RUNNING, this.gameRunning, running);
        }
        this.gameRunning = running;
        game.setGameRunning(running);
    }

    private void registerSlot(SaveData payload) {
        String slotName = payload.slotName;
        long timestamp = payload.timestamp;
        if (!saveSlots.contains(slotName, false)) {
            saveSlots.add(slotName);
        }
        String desc = "Level: " + (payload.levelName != null ? payload.levelName : "Unknown") + 
                      "\nSaved on " + dateFormat.format(new Date(timestamp));
        saveDetails.put(slotName, desc);
        saveTimestamps.put(slotName, timestamp);
        sortSlotsByTime();
    }

    private void loadExistingSaves() {
        saveSlots.clear();
        saveDetails.clear();
        saveTimestamps.clear();
        if (!saveDirectory.exists()) {
            return;
        }
        for (FileHandle file : saveDirectory.list("json")) {
            try {
                SaveData payload = json.fromJson(SaveData.class, file);
                if (payload.timestamp == 0) payload.timestamp = file.lastModified();
                registerSlot(payload);
            } catch (Exception e) {
                Gdx.app.error("Menu", "Failed to read save file " + file.name(), e);
            }
        }
    }

    private SaveData writeSlot(String slotName, long timestamp) {
        GameScreen gameScreen = game.getScreen(GameScreen.class);
        if (gameScreen != null) {
            SaveData data = gameScreen.createSaveData(slotName);
            data.timestamp = timestamp;
            FileHandle file = slotFile(slotName);
            file.writeString(json.toJson(data), false);
            return data;
        } else {
             Gdx.app.error("Menu", "Cannot save: GameScreen not found");
             return null;
        }
    }

    private FileHandle slotFile(String slotName) {
        String fileSafe = slotName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-_]", "_");
        if (fileSafe.isEmpty()) {
            fileSafe = "slot";
        }
        return saveDirectory.child(fileSafe + ".json");
    }

    private void sortSlotsByTime() {
        saveSlots.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                long t1 = saveTimestamps.get(o1, 0L);
                long t2 = saveTimestamps.get(o2, 0L);
                return Long.compare(t2, t1);
            }
        });
    }


}
