package org.samuel.tiling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for generated tiles.
 */
public class TileGrid {

    private final List<Tile> tiles = new ArrayList<>();

    public void add(Tile tile) {
        tiles.add(tile);
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }
}
