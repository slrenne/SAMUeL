package org.samuel.prompts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptBuilderTest {

    @Test
    void storesPointsAndBoxes() {
        PromptBuilder builder = new PromptBuilder();
        builder.addPoint(10, 20, 1);
        builder.addBox(1, 2, 3, 4);
        assertEquals(1, builder.getPoints().size());
        assertEquals(1, builder.getBoxes().size());
        assertEquals(10.0, builder.getPoints().get(0).get(0));
    }
}
