package eu.epitech.lil7_games.component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import eu.epitech.lil7_games.asset.AssetService;
import eu.epitech.lil7_games.asset.AtlasAsset;

public class AnimationSystem extends IteratingSystem {
    private static final float FRAME_DURATION = 1 / 8f; // default for non-attack
    private static final float ATTACK_FRAME_DURATION = 1 / 16f; // faster attack animation

    public final AssetService assetService;
    private final Map<CacheKey, Animation<TextureRegion>> animationCache;
    private static final Map<Animation2D.AnimationType, String[]> TYPE_ALIASES = buildTypeAliases();

    public AnimationSystem(AssetService assetService) {
        super (Family.all(Animation2D.class, Graphic.class, Facing.class).get());
        this.assetService = assetService;
        this.animationCache = new HashMap<>();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Animation2D animation2D = Animation2D.MAPPER.get(entity);
        Facing.FacingDirection facingDirection = Facing.MAPPER.get(entity).getDirection();
        final float stateTime;
        // Avoid repeatedly resetting the DEATH animation if the facing direction
        // toggles while the animation is playing. Only rebuild/reset state time
        // when the component is dirty (explicit type change) or when the
        // facing direction has changed for non-DEATH animations.
        boolean facingChanged = (facingDirection != animation2D.getDirection());
        if (animation2D.isDirty() || (facingChanged && animation2D.getType() != Animation2D.AnimationType.DEATH)) {
            updateAnimation(animation2D, facingDirection);
            stateTime = 0f;
        } else {
            stateTime = animation2D.incAndGetStateTime(deltaTime);
        }

        Animation<TextureRegion> animation = animation2D.getAnimation();
        // Ensure death animations always play once
        if (animation2D.getType() == Animation2D.AnimationType.DEATH) {
            animation.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
        } else {
            animation.setPlayMode(animation2D.getPlayMode());
        }
        TextureRegion keyFrame = animation.getKeyFrame(stateTime);
        Graphic.MAPPER.get(entity).setRegion(keyFrame);

    }

    private void updateAnimation(Animation2D animation2D, Facing.FacingDirection facingDirection) {
        AtlasAsset atlasAsset = animation2D.getAtlasAsset();
        String atlasKey = animation2D.getAtlasKey();
        Animation2D.AnimationType type = animation2D.getType();
        int comboStage = (type == Animation2D.AnimationType.ATTACK) ? animation2D.getAttackComboStage() : 0;
        CacheKey cacheKey = new CacheKey(atlasAsset, atlasKey, type, facingDirection, comboStage);
        Animation<TextureRegion> animation = animationCache.computeIfAbsent(cacheKey, key -> buildAnimation(atlasAsset, atlasKey, type, facingDirection, comboStage));
        animation2D.setAnimation(animation, facingDirection);
    }

    private Animation<TextureRegion> buildAnimation(AtlasAsset atlasAsset, String atlasKey, Animation2D.AnimationType type, Facing.FacingDirection direction, int comboStage) {
        TextureAtlas textureAtlas = this.assetService.get(atlasAsset);
        // Attempt directional naming first (future-proof if assets gain left/right variants)
        String directionalKey = atlasKey + "/" + type.getAtlasKey() + "_" + direction.getAtlasKey();
        Array<TextureAtlas.AtlasRegion> regions = textureAtlas.findRegions(directionalKey);
        if (!regions.isEmpty()) {
            float fd = (type == Animation2D.AnimationType.ATTACK) ? ATTACK_FRAME_DURATION : FRAME_DURATION;
            return new Animation<>(fd, regions);
        }

        String prefix = atlasKey + "/" + type.getAtlasKey();
        Array<TextureAtlas.AtlasRegion> collected = collectByPrefix(textureAtlas, prefix);
        if (collected.isEmpty()) {
            collected = findRegionsByAliases(textureAtlas, atlasKey, type);
        }
        if (collected.isEmpty()) {
            // Try more generous fallbacks before failing outright to make the system resilient
            // 1) Try leaf-only prefix (e.g., ennemi/slime/slime)
            String sanitizedKey = sanitize(atlasKey);
            String leaf = sanitizedKey;
            int slash = sanitizedKey.lastIndexOf('/');
            if (slash >= 0 && slash < sanitizedKey.length() - 1) {
                leaf = sanitizedKey.substring(slash + 1);
            }
            Array<TextureAtlas.AtlasRegion> fallback = collectByPrefix(textureAtlas, sanitizedKey + "/" + leaf);
            if (!fallback.isEmpty()) {
                collected = fallback;
            }
        }

        // If still empty, try to return any region under the atlasKey path (best-effort)
        if (collected.isEmpty()) {
            Array<TextureAtlas.AtlasRegion> anyUnderKey = collectByPrefix(textureAtlas, sanitize(atlasKey) + "/");
            if (!anyUnderKey.isEmpty()) {
                collected = anyUnderKey;
            }
        }

        // Last resort: try to find any region whose name contains the leaf portion
        if (collected.isEmpty()) {
            String sanitizedKey = sanitize(atlasKey);
            String leaf = sanitizedKey;
            int slash = sanitizedKey.lastIndexOf('/');
            if (slash >= 0 && slash < sanitizedKey.length() - 1) {
                leaf = sanitizedKey.substring(slash + 1);
            }
            for (TextureAtlas.AtlasRegion region : textureAtlas.getRegions()) {
                if (sanitize(region.name).toLowerCase(Locale.ROOT).contains(leaf.toLowerCase(Locale.ROOT))) {
                    Array<TextureAtlas.AtlasRegion> found = new Array<>();
                    found.add(region);
                    collected = found;
                    break;
                }
            }
        }

        // If still empty, as a final safety, use the first region in the atlas (avoid throwing)
        if (collected.isEmpty()) {
            Array<TextureAtlas.AtlasRegion> all = textureAtlas.getRegions();
            if (all != null && !all.isEmpty()) {
                Array<TextureAtlas.AtlasRegion> single = new Array<>();
                single.add(all.get(0));
                collected = single;
            } else {
                // no regions at all in atlas; throw with informative message
                throw new GdxRuntimeException("No regions available in atlas for key: '" + atlasKey + "'");
            }
        }

        // Numeric-aware sort so Attack10 goes after Attack9 (not between 1 and 2)
        collected.sort((a, b) -> compareByNumericSuffix(a.name, b.name));

        // For attack type, subset frames based on combo stage
        Array<TextureAtlas.AtlasRegion> finalFrames = collected;
        if (type == Animation2D.AnimationType.ATTACK && comboStage >= 1 && comboStage <= 3) {
            Array<TextureAtlas.AtlasRegion> subset = new Array<>();
            for (TextureAtlas.AtlasRegion r : collected) {
                int num = extractTrailingNumber(r.name.replace(" ", ""));
                if (num == -1) { subset.add(r); continue; }
                boolean include = switch (comboStage) {
                    case 1 -> (num >= 1 && num <= 6);
                    case 2 -> (num >= 7 && num <= 11);
                    case 3 -> (num >= 12 && num <= 17);
                    default -> true;
                };
                if (include) subset.add(r);
            }
            if (subset.isEmpty()) { // fallback use all frames
                subset = collected;
            }
            finalFrames = subset;
        }
        float fd = (type == Animation2D.AnimationType.ATTACK) ? ATTACK_FRAME_DURATION : FRAME_DURATION;
        return new Animation<>(fd, finalFrames);
    }

    /**
     * Ensure the Animation for the given Animation2D is resolved (built and cached)
     * and set on the component. Returns the resolved Animation (never null).
     */
    public Animation<TextureRegion> ensureAnimation(Animation2D animation2D, Facing.FacingDirection facingDirection) {
        if (animation2D == null) return null;
        AtlasAsset atlasAsset = animation2D.getAtlasAsset();
        String atlasKey = animation2D.getAtlasKey();
        Animation2D.AnimationType type = animation2D.getType();
        int comboStage = (type == Animation2D.AnimationType.ATTACK) ? animation2D.getAttackComboStage() : 0;
        CacheKey cacheKey = new CacheKey(atlasAsset, atlasKey, type, facingDirection, comboStage);
        Animation<TextureRegion> animation = animationCache.computeIfAbsent(cacheKey, key -> buildAnimation(atlasAsset, atlasKey, type, facingDirection, comboStage));
        animation2D.setAnimation(animation, facingDirection);
        return animation;
    }

    public record CacheKey(AtlasAsset atlasAsset,
                           String atlasKey,
                           Animation2D.AnimationType type,
                           Facing.FacingDirection direction,
                           int comboStage
    ) {}

    private static int compareByNumericSuffix(String a, String b) {
        String sa = a.replace(" ", "");
        String sb = b.replace(" ", "");
        int na = extractTrailingNumber(sa);
        int nb = extractTrailingNumber(sb);
        if (na != -1 && nb != -1 && !sa.equals(sb)) {
            // Compare prefix (portion before number) first to keep different animation groups separated
            String pa = sa.substring(0, sa.length() - String.valueOf(na).length());
            String pb = sb.substring(0, sb.length() - String.valueOf(nb).length());
            int prefixCmp = pa.compareTo(pb);
            if (prefixCmp != 0) return prefixCmp;
            return Integer.compare(na, nb);
        }
        return sa.compareTo(sb);
    }

    private static int extractTrailingNumber(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isDigit(s.charAt(i))) {
            i--;
        }
        if (i == s.length() - 1) return -1; // no digits
        try {
            return Integer.parseInt(s.substring(i + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Map<Animation2D.AnimationType, String[]> buildTypeAliases() {
        EnumMap<Animation2D.AnimationType, String[]> aliases = new EnumMap<>(Animation2D.AnimationType.class);
        // Provide multiple common aliases to make the system tolerant to atlas naming variations
        aliases.put(Animation2D.AnimationType.IDLE, new String[] {"idle", "stand", "rest"});
        aliases.put(Animation2D.AnimationType.WALK, new String[] {"walk", "move", "step"});
        aliases.put(Animation2D.AnimationType.RUN, new String[] {"run", "sprint", "move"});
        aliases.put(Animation2D.AnimationType.ATTACK, new String[] {"attack", "hit", "slash"});
        aliases.put(Animation2D.AnimationType.DEFEND, new String[] {"defend", "hurt", "stagger"});
        // DEATH commonly appears as death/die/dead; include several variants
        aliases.put(Animation2D.AnimationType.DEATH, new String[] {"death", "die", "dead", "died", "destroy"});
        aliases.put(Animation2D.AnimationType.JUMP, new String[] {"jump", "hop"});
        return aliases;
    }

    private Array<TextureAtlas.AtlasRegion> collectByPrefix(TextureAtlas atlas, String prefix) {
        Array<TextureAtlas.AtlasRegion> collected = new Array<>();
        if (prefix == null || prefix.isEmpty()) {
            return collected;
        }
        String normalized = sanitize(prefix).toLowerCase(Locale.ROOT);
        for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
            String sanitized = sanitize(region.name);
            if (sanitized.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                collected.add(region);
            }
        }
        return collected;
    }

    private Array<TextureAtlas.AtlasRegion> findRegionsByAliases(TextureAtlas textureAtlas,
                                                                 String atlasKey,
                                                                 Animation2D.AnimationType type) {
        Array<TextureAtlas.AtlasRegion> empty = new Array<>();
        String[] aliases = TYPE_ALIASES.get(type);
        if (aliases == null || aliases.length == 0) {
            return empty;
        }

        String sanitizedKey = sanitize(atlasKey);
        String leaf = sanitizedKey;
        int slash = sanitizedKey.lastIndexOf('/');
        if (slash >= 0 && slash < sanitizedKey.length() - 1) {
            leaf = sanitizedKey.substring(slash + 1);
        }

        for (String alias : aliases) {
            if (alias == null || alias.isEmpty()) {
                continue;
            }
            String sanitizedAlias = sanitize(alias);
            if (sanitizedAlias.isEmpty()) {
                continue;
            }
            // Try multiple common naming patterns to be tolerant to atlas naming conventions
            Array<TextureAtlas.AtlasRegion> matches = collectByPrefix(textureAtlas, sanitizedKey + "/" + leaf + "-" + sanitizedAlias);
            if (!matches.isEmpty()) return matches;

            matches = collectByPrefix(textureAtlas, sanitizedKey + "/" + leaf + "_" + sanitizedAlias);
            if (!matches.isEmpty()) return matches;

            matches = collectByPrefix(textureAtlas, sanitizedKey + "/" + leaf + sanitizedAlias);
            if (!matches.isEmpty()) return matches;

            matches = collectByPrefix(textureAtlas, sanitizedKey + "/" + sanitizedAlias + "-" + leaf);
            if (!matches.isEmpty()) return matches;

            matches = collectByPrefix(textureAtlas, sanitizedKey + "/" + sanitizedAlias);
            if (!matches.isEmpty()) return matches;

            // Also try more relaxed contains-based matching
            for (TextureAtlas.AtlasRegion region : textureAtlas.getRegions()) {
                String normalized = sanitize(region.name).toLowerCase(Locale.ROOT);
                if (normalized.contains(leaf.toLowerCase(Locale.ROOT)) && normalized.contains(sanitizedAlias.toLowerCase(Locale.ROOT))) {
                    Array<TextureAtlas.AtlasRegion> found = new Array<>();
                    found.add(region);
                    return found;
                }
            }
        }

        // If no alias matched, try a looser match by leaf alone (e.g., slime-idle vs. slime_idle)
        Array<TextureAtlas.AtlasRegion> byLeaf = collectByPrefix(textureAtlas, sanitizedKey + "/" + leaf);
        if (!byLeaf.isEmpty()) return byLeaf;

        // Nothing found by alias — return empty to allow caller to attempt other fallbacks
        return empty;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace(" ", "");
    }
}
