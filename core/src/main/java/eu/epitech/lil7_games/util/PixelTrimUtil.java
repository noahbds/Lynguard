package eu.epitech.lil7_games.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public final class PixelTrimUtil {
    private PixelTrimUtil() {}

    public static final class PaddingResult {
        public final int bottomTrim;
        public final int leftTrim;
        public final int rightTrim;
        public final int regionPixelWidth;
        public final int regionPixelHeight;
        public PaddingResult(int bottomTrim, int leftTrim, int rightTrim, int w, int h) {
            this.bottomTrim = bottomTrim;
            this.leftTrim = leftTrim;
            this.rightTrim = rightTrim;
            this.regionPixelWidth = w;
            this.regionPixelHeight = h;
        }
    }

    public static PaddingResult computePadding(TextureRegion region, int alphaThreshold) {
        if (region == null) return new PaddingResult(0,0,0,0,0);
        Texture texture = region.getTexture();
        if (texture == null) return new PaddingResult(0,0,0,0,0);
        TextureData data = texture.getTextureData();
        try {
            if (!data.isPrepared()) {
                data.prepare();
            }
            Pixmap pixmap = data.consumePixmap();
            int startX = region.getRegionX();
            int startY = region.getRegionY();
            int width = region.getRegionWidth();
            int height = region.getRegionHeight();

            int bottomTrim = 0;
            for (int localY = 0; localY < height; localY++) {
                int rowY = startY + (height - 1 - localY);
                boolean rowTransparent = true;
                for (int x = 0; x < width; x++) {
                    int pixel = pixmap.getPixel(startX + x, rowY);
                    int alpha = pixel & 0xFF;
                    if (alpha > alphaThreshold) { rowTransparent = false; break; }
                }
                if (rowTransparent) bottomTrim++; else break;
            }

            int leftTrim = 0;
            for (int localX = 0; localX < width; localX++) {
                int colX = startX + localX;
                boolean colTransparent = true;
                for (int y = 0; y < height; y++) {
                    int pixel = pixmap.getPixel(colX, startY + y);
                    int alpha = pixel & 0xFF;
                    if (alpha > alphaThreshold) { colTransparent = false; break; }
                }
                if (colTransparent) leftTrim++; else break;
            }

            int rightTrim = 0;
            for (int localX = 0; localX < width; localX++) {
                int colX = startX + (width - 1 - localX);
                boolean colTransparent = true;
                for (int y = 0; y < height; y++) {
                    int pixel = pixmap.getPixel(colX, startY + y);
                    int alpha = pixel & 0xFF;
                    if (alpha > alphaThreshold) { colTransparent = false; break; }
                }
                if (colTransparent) rightTrim++; else break;
            }

            pixmap.dispose();
            if (leftTrim + rightTrim >= width) { leftTrim = 0; rightTrim = 0; }
            if (bottomTrim >= height) { bottomTrim = 0; }
            return new PaddingResult(bottomTrim, leftTrim, rightTrim, width, height);
        } catch (Exception ex) {
            Gdx.app.error("PixelTrim", "Failed to compute padding: " + ex.getMessage(), ex);
            return new PaddingResult(0,0,0,0,0);
        }
    }
}
