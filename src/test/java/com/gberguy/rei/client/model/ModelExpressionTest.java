package com.gberguy.rei.client.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ModelExpressionTest {
    @Test
    public void molangUsesDegreesAndLiveAnimationTime() {
        assertEquals(1, ModelExpression.compile("math.sin(query.anim_time * 120)").applyAsDouble(0.75), 1e-9);
        assertEquals(-1, ModelExpression.compile("math.cos(query.anim_time*60)").applyAsDouble(3), 1e-9);
        assertEquals(96, ModelExpression.compile("93-3*math.clamp(math.cos(query.anim_time*60)*1.6,-1,1)").applyAsDouble(3), 1e-9);
        assertEquals(6, ModelExpression.compile("-(-2)*3 + 4/2 - 2").applyAsDouble(0), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownQueriesAreRejectedRatherThanSilentlyChangingThePose() {
        ModelExpression.compile("query.modified_distance_moved * 5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidValuesAreRejected() {
        ModelExpression.compile("1/0").applyAsDouble(0);
    }
}
