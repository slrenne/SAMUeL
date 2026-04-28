package org.samuel.objects;

import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import qupath.lib.geom.Point2;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts masks into QuPath objects.
 */
public class MaskToPathObject {

    public List<PathObject> convert(boolean[][] mask, int offsetX, int offsetY, String outputType, int minArea) {
        List<PathObject> outputs = new ArrayList<>();
        List<Point2> contour = extractContour(mask);
        if (contour.size() < 3) {
            return outputs;
        }
        // Shift contour by offset
        List<Point2> shiftedContour = new ArrayList<>();
        for (Point2 p : contour) {
            shiftedContour.add(new Point2(p.getX() + offsetX, p.getY() + offsetY));
        }
        ROI roi = ROIs.createPolygonROI(shiftedContour, null);
        PathObject object = "annotations".equalsIgnoreCase(outputType)
                ? PathObjects.createAnnotationObject(roi)
                : PathObjects.createDetectionObject(roi);
        outputs.add(object);
        return outputs;
    }

    private List<Point2> extractContour(boolean[][] mask) {
        int h = mask.length;
        if (h == 0) return new ArrayList<>();
        int w = mask[0].length;
        boolean[][] visited = new boolean[h][w];
        List<Point2> contour = new ArrayList<>();

        // Find starting point
        int startX = -1, startY = -1;
        for (int y = 0; y < h && startY == -1; y++) {
            for (int x = 0; x < w && startY == -1; x++) {
                if (mask[y][x]) {
                    startX = x;
                    startY = y;
                    break;
                }
            }
        }
        if (startY == -1) return contour;

        // Simple boundary tracing (Moore-Neighbor)
        int x = startX, y = startY;
        int dir = 0; // 0: right, 1: down, 2: left, 3: up
        int[] dx = {1, 0, -1, 0};
        int[] dy = {0, 1, 0, -1};

        do {
            contour.add(new Point2(x, y));
            visited[y][x] = true;

            boolean found = false;
            for (int i = 0; i < 4; i++) {
                int ndir = (dir + i) % 4;
                int nx = x + dx[ndir];
                int ny = y + dy[ndir];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && mask[ny][nx] && !visited[ny][nx]) {
                    x = nx;
                    y = ny;
                    dir = ndir;
                    found = true;
                    break;
                }
            }
            if (!found) break;
        } while (x != startX || y != startY);

        return contour;
    }
}
