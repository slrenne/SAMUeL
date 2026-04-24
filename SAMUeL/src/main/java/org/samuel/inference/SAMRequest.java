package org.samuel.inference;

import java.util.ArrayList;
import java.util.List;

/**
 * REST payload sent to Python backend.
 */
public class SAMRequest {

    private String tileId;
    private String image;
    private String modelType;
    private boolean useGpu = true;
    private final List<List<Double>> points = new ArrayList<>();
    private final List<List<Double>> boxes = new ArrayList<>();

    public String getTileId() {
        return tileId;
    }

    public void setTileId(String tileId) {
        this.tileId = tileId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<List<Double>> getPoints() {
        return points;
    }

    public List<List<Double>> getBoxes() {
        return boxes;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public void setUseGpu(boolean useGpu) {
        this.useGpu = useGpu;
    }
}
