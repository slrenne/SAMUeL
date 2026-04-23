package org.samuel.tiling;

/**
 * Represents one image tile in local and global coordinates.
 */
public record Tile(String id, int x, int y, int width, int height) {
}
