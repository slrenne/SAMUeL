package org.samuel.prompts;

import qupath.lib.objects.PathObject;
import qupath.lib.roi.PolygonROI;
import qupath.lib.roi.interfaces.ROI;

import java.util.List;

/**
 * Converts QuPath annotations into SAM prompts.
 */
public class AnnotationPromptConverter {

    public PromptBuilder fromAnnotations(List<PathObject> promptAnnotations) {
        PromptBuilder builder = new PromptBuilder();
        for (PathObject object : promptAnnotations) {
            ROI roi = object.getROI();
            if (roi == null) {
                continue;
            }
            if (roi.isPoint()) {
                builder.addPoint(roi.getCentroidX(), roi.getCentroidY(), 1);
            } else if (roi.isArea() && roi.getBoundsWidth() > 0 && roi.getBoundsHeight() > 0 && !(roi instanceof PolygonROI)) {
                builder.addBox(
                        roi.getBoundsX(),
                        roi.getBoundsY(),
                        roi.getBoundsX() + roi.getBoundsWidth(),
                        roi.getBoundsY() + roi.getBoundsHeight()
                );
            } else if (roi instanceof PolygonROI) {
                var points = roi.getAllPoints();
                int n = Math.max(1, points.size() / 8);
                for (int i = 0; i < points.size(); i += n) {
                    builder.addPoint(points.get(i).getX(), points.get(i).getY(), 1);
                }
            } else {
                builder.addPoint(roi.getCentroidX(), roi.getCentroidY(), 1);
            }
        }
        return builder;
    }
}
