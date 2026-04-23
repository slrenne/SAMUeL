package org.samuel.prompts;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds SAM prompts from generic geometric primitives.
 */
public class PromptBuilder {

    private final List<List<Double>> points = new ArrayList<>();
    private final List<List<Double>> boxes = new ArrayList<>();

    public void addPoint(double x, double y, int label) {
        points.add(List.of(x, y, (double) label));
    }

    public void addBox(double x1, double y1, double x2, double y2) {
        boxes.add(List.of(x1, y1, x2, y2));
    }

    public List<List<Double>> getPoints() {
        return points;
    }

    public List<List<Double>> getBoxes() {
        return boxes;
    }
}
