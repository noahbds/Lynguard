package eu.epitech.lil7_games.save;

import java.util.ArrayList;
import java.util.List;

public class SaveData {
    // Nom du slot (ex: "slot1") affiché dans l'UI.
    public String slotName;
    // Horodatage de la sauvegarde (millis depuis epoch) pour tri chronologique.
    public long timestamp;
    // Nom du niveau courant (Enum MapAsset.name()).
    public String levelName;
    // Position du joueur (monde) au moment de la sauvegarde.
    public float playerX;
    public float playerY;
    // PV actuels et maximum (maxLife permet de restaurer upgrades éventuels).
    public float playerLife;
    public float playerMaxLife;
    // Clés possédées (persistantes entre respawn/niveaux).
    public List<String> playerKeys = new ArrayList<>();
    // Ennemis morts indexés par nom de niveau -> liste IDs (pour éviter respawn).
    public java.util.Map<String, List<String>> deadEnemies = new java.util.HashMap<>();
    public SaveData() {}
}
