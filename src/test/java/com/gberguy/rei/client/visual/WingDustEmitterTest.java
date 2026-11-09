package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import com.gberguy.rei.client.model.BlockbenchModelTest;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class WingDustEmitterTest {
    @Test
    public void fourSitesOnEachWingExtendProgressivelyOutFromTheBody() throws Exception {
        BlockbenchModel model = BlockbenchModelTest.model();
        WingDustEmitter emitter = new WingDustEmitter(model);
        double[][] pose = model.animations.get(BlockbenchModel.IDLE).pose(model.bones.size(), 0);
        double[][] points = emitter.points(pose, new double[3], 0, 0.14, new Random(1));
        assertEquals(8, points.length);
        for (int wing = 0; wing < 2; wing++) {
            double previous = 0;
            for (int site = 0; site < 4; site++) {
                double[] point = points[wing * 4 + site];
                assertTrue(Math.abs(point[0]) > previous);
                previous = Math.abs(point[0]);
                assertTrue(point[1] > -0.14 * 0.4);
                assertTrue(wing == 0 ? point[0] < 0 : point[0] > 0);
            }
        }
    }

    @Test
    public void wingSitesFollowAnimationAndScaleRotateAndTranslateWithTheCompanion() throws Exception {
        BlockbenchModel model = BlockbenchModelTest.model();
        WingDustEmitter emitter = new WingDustEmitter(model);
        double[][] pose = model.animations.get(BlockbenchModel.IDLE).pose(model.bones.size(), 0);
        double[][] first = emitter.points(pose, new double[3], 0, 0.14, new Random(1));
        double[][] translated = emitter.points(pose, new double[] {10,20,30}, 0, 0.14, new Random(1));
        double[][] scaled = emitter.points(pose, new double[3], 0, 0.28, new Random(1));
        double[][] rotated = emitter.points(pose, new double[3], 90, 0.14, new Random(1));
        for (int site = 0; site < 8; site++) {
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(first[site][axis] + (axis + 1) * 10, translated[site][axis], 1e-9);
                assertEquals(first[site][axis] * 2, scaled[site][axis], 1e-9);
            }
            assertArrayEquals(new double[] {-first[site][2], first[site][1], first[site][0]}, rotated[site], 1e-9);
        }
        double[][] later = emitter.points(model.animations.get(BlockbenchModel.IDLE).pose(model.bones.size(), 1.5),
                new double[3], 0, 0.14, new Random(1));
        boolean changed = false;
        for (int site = 0; site < 8; site++) for (int axis = 0; axis < 3; axis++) changed |= Math.abs(first[site][axis] - later[site][axis]) > 1e-5;
        assertTrue(changed);
    }

    @Test
    public void eachSiteCanEmitAndMinimalParticleSettingsDisableTheEmission() throws Exception {
        BlockbenchModel model = BlockbenchModelTest.model();
        WingDustEmitter emitter = new WingDustEmitter(model);
        double[][] pose = model.animations.get(BlockbenchModel.IDLE).pose(model.bones.size(), 0);
        Random alwaysEmit = new Random(1) { @Override public int nextInt(int bound) { return 0; } };
        assertEquals(8, emitter.emissions(pose, new double[3], 0, 0.14, 0, alwaysEmit).length);
        assertEquals(0, emitter.emissions(pose, new double[3], 0, 0.14, 2, alwaysEmit).length);
        Random random = new Random(2);
        int count = 0;
        for (int round = 0; round < 200; round++) count += emitter.emissions(pose, new double[3], 0, 0.14, 0, random).length;
        assertTrue(count > 200 && count < 600);
    }

    @Test
    public void aModelWithoutRecognizedWingsDoesNotFallBackToDustFromTheBody() throws Exception {
        com.google.gson.JsonObject source = BlockbenchModelTest.source();
        for (com.google.gson.JsonElement value : source.getAsJsonArray("groups")) {
            com.google.gson.JsonObject bone = value.getAsJsonObject();
            if (bone.get("name").getAsString().endsWith("_wing")) bone.addProperty("name", "other");
        }
        BlockbenchModel model = BlockbenchModelTest.parse(source);
        assertEquals(0, new WingDustEmitter(model).points(model.animations.get(BlockbenchModel.IDLE).pose(model.bones.size(), 0),
                new double[3], 0, 0.14, new Random(1)).length);
    }
}
