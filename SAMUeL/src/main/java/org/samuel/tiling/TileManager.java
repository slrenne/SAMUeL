package org.samuel.tiling;

import org.samuel.objects.GeometryUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Tile generation and coordinate translation utilities.
 */
public class TileManager {

    public TileGrid generateGrid(int x, int y, int width, int height, int tileSize, int overlap) {
        TileGrid grid = new TileGrid();
        int stride = Math.max(1, tileSize - overlap);
        int index = 0;
        for (int ty = y; ty < y + height; ty += stride) {
            for (int tx = x; tx < x + width; tx += stride) {
                int tw = Math.min(tileSize, x + width - tx);
                int th = Math.min(tileSize, y + height - ty);
                if (tw > 0 && th > 0) {
                    grid.add(new Tile("tile-" + index++, tx, ty, tw, th));
                }
            }
        }
        return grid;
    }

    public List<boolean[][]> mergeMasks(List<boolean[][]> masks) {
        // Current implementation keeps each object as an independent mask while
        // cleaning disconnected speckles; overlap reconciliation is done through IOU filtering.
        List<boolean[][]> cleaned = new ArrayList<>();
        for (boolean[][] mask : masks) {
            cleaned.add(GeometryUtils.keepLargestConnectedComponent(mask));
        }
        return cleaned;
    }

    public BufferedImage crop(BufferedImage image, Tile tile) {
        return image.getSubimage(tile.x(), tile.y(), tile.width(), tile.height());
    }
}
