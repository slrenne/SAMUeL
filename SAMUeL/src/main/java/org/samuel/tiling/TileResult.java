package org.samuel.tiling;

import org.samuel.inference.SAMResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Inference result for one tile.
 */
public class TileResult {

    private final Tile tile;
    private final List<SAMResponse.MaskPayload> masks = new ArrayList<>();

    public TileResult(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }

    public List<SAMResponse.MaskPayload> getMasks() {
        return masks;
    }
}
