package org.samuel.objects;

import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts masks into QuPath objects.
 */
public class MaskToPathObject {

    public List<PathObject> convert(boolean[][] mask, int offsetX, int offsetY, String outputType, int minArea) {
        List<PathObject> outputs = new ArrayList<>();
        int h = mask.length;
        if (h == 0) {
            return outputs;
        }
        int w = mask[0].length;
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (mask[y][x]) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return outputs;
        }
        int area = (maxX - minX + 1) * (maxY - minY + 1);
        if (area < minArea) {
            return outputs;
        }
        ROI roi = ROIs.createRectangleROI(offsetX + minX, offsetY + minY, maxX - minX + 1, maxY - minY + 1, null);
        PathObject object = "annotations".equalsIgnoreCase(outputType)
                ? PathObjects.createAnnotationObject(roi)
                : PathObjects.createDetectionObject(roi);
        outputs.add(object);
        return outputs;
    }
}
