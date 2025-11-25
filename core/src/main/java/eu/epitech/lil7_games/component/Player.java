package eu.epitech.lil7_games.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Composant Player.
 * Stocke actuellement uniquement les "keys" collectées (identifiants logiques d'objets/récompenses).
 * TODO: Éventuellement déplacer inventaire / capacités dans des composants dédiés (Inventory, Abilities...).
 */
public class Player implements Component {
    public static final ComponentMapper<Player> MAPPER = ComponentMapper.getFor(Player.class);

    // Liste des clés persistantes collectées par le joueur (utilisée pour HUD + transitions).
    public java.util.List<String> keys = new java.util.ArrayList<>();
}
