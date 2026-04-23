package org.samuel.objects;

import java.util.Base64;

/**
 * Decodes binary masks serialized as base64-packed bytes.
 */
public class MaskDecoder {

    public boolean[][] decode(String base64Data, int width, int height) {
        byte[] bytes = Base64.getDecoder().decode(base64Data);
        boolean[][] mask = new boolean[height][width];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (index < bytes.length) {
                    mask[y][x] = bytes[index++] != 0;
                }
            }
        }
        return mask;
    }
}
