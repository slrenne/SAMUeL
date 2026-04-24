package org.samuel.inference;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * REST payload sent to Python backend.
 */
public class SAMRequest {

    @JsonProperty("tile_id")
    private String tileId;

    @JsonProperty("image")
    private String image;

    @JsonProperty("model_type")
    private String modelType;

    @JsonProperty("use_gpu")
    private boolean useGpu = true;

    @JsonProperty("points")
    private final List<List<Double>> points = new ArrayList<>();

    @JsonProperty("boxes")
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