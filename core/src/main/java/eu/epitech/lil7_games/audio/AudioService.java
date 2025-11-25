package eu.epitech.lil7_games.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.MathUtils;

import eu.epitech.lil7_games.asset.AssetService;
import eu.epitech.lil7_games.Lynguard;
import eu.epitech.lil7_games.asset.MusicAsset;
import eu.epitech.lil7_games.asset.SoundAsset;

public class AudioService {

    private final AssetService assetService;
    private Music currentMusic;
    private MusicAsset currentMusicAsset;
    private float musicVolume;
    private float soundVolume;
    private final java.util.Map<SoundAsset, Long> loopingSounds;
    private final java.util.Map<Long, PositionalEmitter> positionalLoopers;
    private final java.util.Map<SoundAsset, Float> baseVolumes;

    public AudioService(AssetService assetService) {
        this.assetService = assetService;
        this.currentMusic = null;
        this.currentMusicAsset = null;
        this.musicVolume = 0.5f;
        this.soundVolume = 0.33f;
        this.loopingSounds = new java.util.HashMap<>();
        this.positionalLoopers = new java.util.HashMap<>();
        this.baseVolumes = new java.util.HashMap<>();
        this.baseVolumes.put(SoundAsset.WATERFALL, 0.28f);
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = MathUtils.clamp(musicVolume, 0.0f, 1.0f);
        if (this.currentMusic != null) {
            this.currentMusic.setVolume(musicVolume);
        }
    }

    public float getMusicVolume() {
        return this.musicVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = MathUtils.clamp(soundVolume, 0.0f, 1.0f);
        try {
            for (java.util.Map.Entry<SoundAsset, Long> entry : loopingSounds.entrySet()) {
                this.assetService.get(entry.getKey()).setVolume(entry.getValue(), this.soundVolume);
            }
        } catch (NoSuchMethodError ignored) {}

        for (java.util.Map.Entry<Long, PositionalEmitter> entry : positionalLoopers.entrySet()) {
            PositionalEmitter e = entry.getValue();
            try {
                this.assetService.get(e.asset).setVolume(e.id, this.soundVolume);
            } catch (NoSuchMethodError ignored) {}
        }
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void playMusic(MusicAsset musicAsset) {
        if (this.currentMusicAsset == musicAsset) {
            return;
        }

        if (this.currentMusic != null) {
            this.currentMusic.stop();
            this.assetService.unload(this.currentMusicAsset);
        }

        this.currentMusic = this.assetService.load(musicAsset);
        this.currentMusic.setLooping(true);
        this.currentMusic.setVolume(musicVolume);
        this.currentMusic.play();
        this.currentMusicAsset = musicAsset;
    }

    public void stopMusic(MusicAsset musicAsset) {
        if (this.currentMusicAsset != musicAsset) {
            return;
        }

        if (this.currentMusic != null) {
            this.currentMusic.stop();
            this.assetService.unload(this.currentMusicAsset);
            this.currentMusic = null;
            this.currentMusicAsset = null;
        }
    }

    public void playSound(SoundAsset soundAsset) {
        this.assetService.load(soundAsset);
        this.assetService.get(soundAsset).play(soundVolume);
    }

    public void playSound(SoundAsset soundAsset, float volume) {
        this.assetService.load(soundAsset);
        float vol = MathUtils.clamp(volume, 0.0f, 1.0f);
        this.assetService.get(soundAsset).play(vol);
    }

    public long playLoopingSound(SoundAsset soundAsset) {
        if (loopingSounds.containsKey(soundAsset)) {
            long existingId = loopingSounds.get(soundAsset);
            this.assetService.get(soundAsset).stop(existingId);
        }

        this.assetService.load(soundAsset);
        long soundId = this.assetService.get(soundAsset).loop(soundVolume);
        loopingSounds.put(soundAsset, soundId);
        return soundId;
    }

    public long playLoopingPositionalSound(SoundAsset soundAsset, float x, float y, float maxDistance, float baseVolume) {
        this.assetService.load(soundAsset);
        long soundId = this.assetService.get(soundAsset).loop(0f);
        PositionalEmitter emitter = new PositionalEmitter(soundAsset, soundId, x, y, maxDistance, baseVolume);
        positionalLoopers.put(soundId, emitter);
        return soundId;
    }

    public void stopLoopingPositional(long soundId) {
        PositionalEmitter emitter = positionalLoopers.remove(soundId);
        if (emitter != null) {
            this.assetService.get(emitter.asset).stop(soundId);
        }
    }

    public void stopLoopingSound(SoundAsset soundAsset, long soundId) {
        this.assetService.get(soundAsset).stop(soundId);
        loopingSounds.remove(soundAsset);
    }

    public void setMap(com.badlogic.gdx.maps.tiled.TiledMap tiledMap) {
        for (java.util.Map.Entry<SoundAsset, Long> entry : new java.util.HashMap<>(loopingSounds).entrySet()) {
            this.assetService.get(entry.getKey()).stop(entry.getValue());
        }
        loopingSounds.clear();
        for (java.util.Map.Entry<Long, PositionalEmitter> entry : new java.util.HashMap<>(positionalLoopers).entrySet()) {
            this.assetService.get(entry.getValue().asset).stop(entry.getKey());
        }
        positionalLoopers.clear();

        String musicAssetStr = tiledMap.getProperties().get("music", "", String.class);
        if (musicAssetStr.isBlank()) {
            for (com.badlogic.gdx.maps.MapLayer layer : tiledMap.getLayers()) {
                String layerMusic = layer.getProperties().get("music", "", String.class);
                if (!layerMusic.isBlank()) {
                    musicAssetStr = layerMusic;
                    break;
                }
            }
        }
        if (!musicAssetStr.isBlank()) {
            try {
                MusicAsset musicAsset = MusicAsset.valueOf(musicAssetStr.trim().toUpperCase());
                playMusic(musicAsset);
            } catch (IllegalArgumentException ex) {
            }
        }

        String soundAssetStr = tiledMap.getProperties().get("sound", "", String.class);
        if (!soundAssetStr.isBlank()) {
            for (String s : soundAssetStr.split(",")) {
                SoundAsset soundAsset = SoundAsset.valueOf(s.trim());
                playLoopingSound(soundAsset);
            }
        }
        int mapTilesW = tiledMap.getProperties().get("width", Integer.class) != null ? tiledMap.getProperties().get("width", Integer.class) : 0;
        int mapTilesH = tiledMap.getProperties().get("height", Integer.class) != null ? tiledMap.getProperties().get("height", Integer.class) : 0;
        int tileW = tiledMap.getProperties().get("tilewidth", Integer.class) != null ? tiledMap.getProperties().get("tilewidth", Integer.class) : 0;
        int tileH = tiledMap.getProperties().get("tileheight", Integer.class) != null ? tiledMap.getProperties().get("tileheight", Integer.class) : 0;
        float mapPixelW = mapTilesW * tileW * Lynguard.UNIT_SCALE;
        float mapPixelH = mapTilesH * tileH * Lynguard.UNIT_SCALE;
        float computedMax = Math.max(mapPixelW, mapPixelH) * 0.6f;
        computedMax = MathUtils.clamp(computedMax, 50f, 1200f);
        for (com.badlogic.gdx.maps.MapLayer layer : tiledMap.getLayers()) {
            String layerSoundStr = layer.getProperties().get("sound", "", String.class);
            if (!layerSoundStr.isBlank()) {
                if (layer.getObjects() != null && layer.getObjects().getCount() > 0) {
                    for (com.badlogic.gdx.maps.MapObject obj : layer.getObjects()) {
                        float ox = obj.getProperties().get("x", Float.class) != null ? obj.getProperties().get("x", Float.class) : 0f;
                        float oy = obj.getProperties().get("y", Float.class) != null ? obj.getProperties().get("y", Float.class) : 0f;
                        for (String s : layerSoundStr.split(",")) {
                            try {
                                SoundAsset soundAsset = SoundAsset.valueOf(s.trim());
                                float base = this.baseVolumes.getOrDefault(soundAsset, 1f);
                                float maxDist = computedMax;
                                if (soundAsset == SoundAsset.WATERFALL) {
                                    maxDist = MathUtils.clamp(computedMax * 0.35f, 30f, computedMax);
                                }
                                playLoopingPositionalSound(soundAsset, ox * Lynguard.UNIT_SCALE, oy * Lynguard.UNIT_SCALE, maxDist, base);
                            } catch (IllegalArgumentException ex) {
                            }
                        }
                    }
                } else {
                    float centerX = mapPixelW / 2f;
                    float centerY = mapPixelH / 2f;
                    for (String s : layerSoundStr.split(",")) {
                        try {
                            SoundAsset soundAsset = SoundAsset.valueOf(s.trim());
                            float base = this.baseVolumes.getOrDefault(soundAsset, 1f);
                            float maxDist = computedMax;
                            if (soundAsset == SoundAsset.WATERFALL) {
                                maxDist = MathUtils.clamp(computedMax * 0.35f, 100f, computedMax);
                            }
                            playLoopingPositionalSound(soundAsset, centerX, centerY, maxDist, base);
                        } catch (IllegalArgumentException ex) {
                        }
                    }
                }
            }
        }
    }

    private static class PositionalEmitter {
        public final SoundAsset asset;
        public final long id;
        public final float x;
        public final float y;
        public final float maxDistance;
        public final float baseVolume;

        public PositionalEmitter(SoundAsset asset, long id, float x, float y, float maxDistance, float baseVolume) {
            this.asset = asset;
            this.id = id;
            this.x = x;
            this.y = y;
            this.maxDistance = maxDistance;
            this.baseVolume = baseVolume;
        }
    }

    /**
     * Update the listener (player) position so positional emitters can adjust volume/pan.
     */
    public void updateListenerPosition(float lx, float ly) {
        for (java.util.Map.Entry<Long, PositionalEmitter> entry : new java.util.HashMap<>(positionalLoopers).entrySet()) {
            PositionalEmitter e = entry.getValue();
            float dx = e.x - lx;
            float dy = e.y - ly;
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            float vol;
            // Use a steeper, non-linear attenuation for WATERFALL so it's less overpowering on small maps
            if (e.asset == SoundAsset.WATERFALL) {
                // sigma ~= 25% of maxDistance -> steep drop
                float sigma = Math.max(1f, e.maxDistance * 0.25f);
                float inv = 1f / (1f + (dist * dist) / (sigma * sigma));
                vol = MathUtils.clamp(soundVolume * e.baseVolume * inv, 0f, 1f);
            } else {
                float atten = 0f;
                if (dist < e.maxDistance) {
                    atten = 1f - (dist / e.maxDistance);
                } else {
                    atten = 0f;
                }
                vol = MathUtils.clamp(soundVolume * e.baseVolume * atten, 0f, 1f);
            }
            float pan = MathUtils.clamp(dx / e.maxDistance, -1f, 1f);
            try {
                this.assetService.get(e.asset).setPan(e.id, pan, vol);
            } catch (NoSuchMethodError ex) {
                // Fallback if setPan not available; try setVolume
                this.assetService.get(e.asset).setVolume(e.id, vol);
            }
        }
    }
}
