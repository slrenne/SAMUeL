package org.samuel.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryUtilsTest {

    @Test
    void keepsLargestConnectedComponent() {
        boolean[][] mask = new boolean[6][6];
        mask[0][0] = true;
        mask[0][1] = true;
        mask[1][0] = true;
        mask[4][4] = true;
        boolean[][] largest = GeometryUtils.keepLargestConnectedComponent(mask);
        assertEquals(3, GeometryUtils.area(largest));
    }
}
