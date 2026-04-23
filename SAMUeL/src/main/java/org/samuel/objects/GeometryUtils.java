package org.samuel.objects;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Geometry and binary-mask utility methods.
 */
public final class GeometryUtils {

    private GeometryUtils() {
    }

    public static int area(boolean[][] mask) {
        int count = 0;
        for (boolean[] row : mask) {
            for (boolean value : row) {
                if (value) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean[][] keepLargestConnectedComponent(boolean[][] mask) {
        int h = mask.length;
        if (h == 0) {
            return mask;
        }
        int w = mask[0].length;
        boolean[][] visited = new boolean[h][w];
        boolean[][] best = new boolean[h][w];
        int bestCount = 0;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!mask[y][x] || visited[y][x]) {
                    continue;
                }
                boolean[][] current = new boolean[h][w];
                int currentCount = 0;
                Queue<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                visited[y][x] = true;
                while (!q.isEmpty()) {
                    int[] p = q.remove();
                    int px = p[0];
                    int py = p[1];
                    current[py][px] = true;
                    currentCount++;
                    for (int i = 0; i < 4; i++) {
                        int nx = px + dx[i];
                        int ny = py + dy[i];
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h && mask[ny][nx] && !visited[ny][nx]) {
                            visited[ny][nx] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                if (currentCount > bestCount) {
                    best = current;
                    bestCount = currentCount;
                }
            }
        }
        return best;
    }
}
