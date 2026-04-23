package org.samuel.tiling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileManagerTest {

    @Test
    void generatesOverlappingTiles() {
        TileManager manager = new TileManager();
        TileGrid grid = manager.generateGrid(0, 0, 2048, 2048, 1024, 128);
        assertTrue(grid.getTiles().size() >= 4);
        Tile first = grid.getTiles().get(0);
        assertEquals(0, first.x());
        assertEquals(0, first.y());
    }
}
