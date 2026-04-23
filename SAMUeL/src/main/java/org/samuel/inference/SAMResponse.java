package org.samuel.inference;

import java.util.ArrayList;
import java.util.List;

/**
 * REST response returned by Python backend.
 */
public class SAMResponse {

    private String tileId;
    private final List<MaskPayload> masks = new ArrayList<>();

    public String getTileId() {
        return tileId;
    }

    public void setTileId(String tileId) {
        this.tileId = tileId;
    }

    public List<MaskPayload> getMasks() {
        return masks;
    }

    public static class MaskPayload {
        private int width;
        private int height;
        private String data;
        private double score;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }
}
