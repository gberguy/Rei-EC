package com.gberguy.rei.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlockbenchModelTest {
    public static JsonObject source() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(BlockbenchModelTest.class.getResourceAsStream("/assets/rei/companion/rei.bbmodel"), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    public static BlockbenchModel model() throws Exception { return parse(source()); }

    public static BlockbenchModel parse(JsonObject json) throws Exception {
        return BlockbenchModel.read(new StringReader(json.toString()));
    }

    @Test
    public void suppliedResourceRetainsHierarchyTexturesAndFiniteAnimatedGeometry() throws Exception {
        JsonObject source = source();
        BlockbenchModel model = parse(source);
        assertEquals(source.getAsJsonArray("groups").size(), model.bones.size());
        assertEquals(source.getAsJsonArray("animations").size(), model.animations.size());
        assertEquals(2, model.textures.size());
        assertTrue(model.height > 0);
        for (int bone = 0; bone < model.bones.size(); bone++) assertTrue(model.bones.get(bone).parent < bone);
        for (java.awt.image.BufferedImage texture : model.textures) { assertEquals(128, texture.getWidth()); assertEquals(128, texture.getHeight()); }
        ModelAnimation idle = model.animations.get(BlockbenchModel.IDLE);
        double[][] first = idle.pose(model.bones.size(), 0);
        double[][] middle = idle.pose(model.bones.size(), idle.length / 2);
        boolean changed = false;
        for (int bone = 0; bone < first.length; bone++) for (int axis = 0; axis < 9; axis++) changed |= Math.abs(first[bone][axis] - middle[bone][axis]) > 1e-5;
        assertTrue(changed);
        for (double time = 0; time < idle.length; time += 0.05) {
            double[][][] vertices = model.vertices(idle.pose(model.bones.size(), time));
            assertFalse(vertices.length == 0);
            for (double[][] cube : vertices) for (double[] vertex : cube) for (double coordinate : vertex) assertTrue(Double.isFinite(coordinate));
        }
    }

    @Test
    public void linearAndBezierTracksInterpolateWithoutDroppingLiveExpressions() throws Exception {
        JsonObject source = source();
        String id = source.getAsJsonArray("groups").get(0).getAsJsonObject().get("uuid").getAsString();
        String json = "{\"name\":\"test\",\"length\":2,\"loop\":\"once\",\"animators\":{\"" + id + "\":{\"keyframes\":["
                + "{\"channel\":\"rotation\",\"time\":0,\"interpolation\":\"bezier\",\"bezier_right_time\":[0.2,0,0],\"data_points\":[{\"x\":\"math.sin(query.anim_time*90)\",\"y\":\"0\",\"z\":\"0\"}]},"
                + "{\"channel\":\"rotation\",\"time\":2,\"interpolation\":\"bezier\",\"bezier_left_time\":[-0.2,0,0],\"data_points\":[{\"x\":\"10+math.sin(query.anim_time*90)\",\"y\":\"0\",\"z\":\"0\"}]}]}}}";
        java.util.Map<String,Integer> ids = java.util.Collections.singletonMap(id, 0);
        ModelAnimation animation = new ModelAnimation(new JsonParser().parse(json).getAsJsonObject(), ids);
        assertEquals(6, animation.pose(1, 1)[0][3], 1e-5);
        assertEquals(0, animation.pose(1, 0)[0][3], 1e-9);
        assertEquals(10, animation.pose(1, 2)[0][3], 1e-9);
    }

    @Test
    public void nestedBoneAndCubePivotsUseBlockbenchZyxTransforms() throws Exception {
        JsonObject source = source();
        JsonParser parser = new JsonParser();
        source.add("groups", parser.parse("[{\"name\":\"root\",\"uuid\":\"root\",\"origin\":[0,0,0],\"rotation\":[0,0,90]},{\"name\":\"child\",\"uuid\":\"child\",\"origin\":[1,0,0],\"rotation\":[0,0,90]}]"));
        source.add("outliner", parser.parse("[{\"uuid\":\"root\",\"children\":[{\"uuid\":\"child\",\"children\":[\"cube\"]}]}]"));
        source.add("elements", parser.parse("[{\"uuid\":\"cube\",\"type\":\"cube\",\"origin\":[2,0,0],\"rotation\":[0,0,90],\"from\":[2,0,0],\"to\":[3,1,1],\"faces\":{\"north\":{\"texture\":0,\"uv\":[0,0,1,1]}}}]"));
        source.add("animations", parser.parse("[{\"name\":\"animation.succubus.idle\",\"length\":1,\"loop\":\"loop\",\"animators\":{}}]"));
        BlockbenchModel model = parse(source);
        double[][][] vertices = model.vertices(model.animations.get(BlockbenchModel.IDLE).pose(2, 0));
        assertArrayEquals(new double[] {-1,1,0}, vertices[0][0], 1e-9);
        assertArrayEquals(new double[] {-1,0,0}, vertices[0][1], 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingIdleFailsClearly() throws Exception {
        JsonObject source = source(); source.add("animations", new com.google.gson.JsonArray()); parse(source);
    }
}
