package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import com.gberguy.rei.client.model.ModelAnimation;
import com.gberguy.rei.client.model.BlockbenchModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualAnimationStateTest {
    @Test
    public void defaultIsIdleWithoutPerFrameRandomSwitches() throws Exception {
        BlockbenchModel model = BlockbenchModelTest.model();
        VisualSettings settings = new VisualSettings();
        VisualAnimationState state = new VisualAnimationState(1, 0, settings);
        for (int frame = 0; frame < 10000; frame++) {
            state.pose(model, frame / 60.0, settings);
            assertEquals(BlockbenchModel.IDLE, state.active());
        }
    }

    @Test
    public void futureWeightedClipsFinishAndBlendBackToIdleWithoutInterruptions() throws Exception {
        BlockbenchModel model = BlockbenchModelTest.model();
        com.google.gson.JsonObject extra = BlockbenchModelTest.source().getAsJsonArray("animations").get(0).getAsJsonObject();
        extra.addProperty("name", "extra");
        model.animations.put("extra", new ModelAnimation(extra, boneIds(BlockbenchModelTest.source())));
        VisualSettings settings = new VisualSettings(); settings.minDelay = 2; settings.maxDelay = 2; settings.weights.put("extra", 1.0);
        VisualAnimationState state = new VisualAnimationState(1, 0, settings);
        double[][] before = state.pose(model, 2 - 1e-8, settings);
        double[][] transition = state.pose(model, 2, settings);
        assertEquals("extra", state.active());
        for (int bone = 0; bone < before.length; bone++) assertArrayEquals(before[bone], transition[bone], 1e-5);
        state.pose(model, 3, settings); assertEquals("extra", state.active());
        state.pose(model, 2 + model.animations.get("extra").length, settings); assertEquals(BlockbenchModel.IDLE, state.active());
        VisualAnimationState other = new VisualAnimationState(1, 100, settings);
        other.pose(model, 100, settings); assertEquals(BlockbenchModel.IDLE, other.active());
    }

    @Test
    public void aLoopMarkedExtraBlendsFromItsEndPoseWithoutWrappingBackToTheStart() throws Exception {
        BlockbenchModel model = BlockbenchModelTest.model();
        String root = BlockbenchModelTest.source().getAsJsonArray("groups").get(0).getAsJsonObject().get("uuid").getAsString();
        String json = "{\"name\":\"extra\",\"length\":1,\"loop\":\"loop\",\"animators\":{\"" + root + "\":{\"keyframes\":["
                + "{\"channel\":\"rotation\",\"time\":0,\"interpolation\":\"linear\",\"data_points\":[{\"x\":\"0\",\"y\":\"0\",\"z\":\"0\"}]},"
                + "{\"channel\":\"rotation\",\"time\":1,\"interpolation\":\"linear\",\"data_points\":[{\"x\":\"90\",\"y\":\"0\",\"z\":\"0\"}]}]}}}";
        model.animations.put("extra", new ModelAnimation(new com.google.gson.JsonParser().parse(json).getAsJsonObject(), java.util.Collections.singletonMap(root, 0)));
        VisualSettings settings = new VisualSettings(); settings.minDelay = 1; settings.maxDelay = 1;
        settings.transitionSeconds = 0.2; settings.weights.put("extra", 1.0);
        VisualAnimationState state = new VisualAnimationState(1, 0, settings);
        state.pose(model, 1, settings);
        assertEquals(81, state.pose(model, 1.9, settings)[0][3], 1e-6);
        assertEquals(90, state.pose(model, 2, settings)[0][3], 1e-6);
        assertEquals(BlockbenchModel.IDLE, state.active());
    }

    private static java.util.Map<String,Integer> boneIds(com.google.gson.JsonObject source) throws Exception {
        BlockbenchModel model = BlockbenchModelTest.parse(source);
        java.util.Map<String,Integer> result = new java.util.HashMap<>();
        for (com.google.gson.JsonElement value : source.getAsJsonArray("groups")) {
            com.google.gson.JsonObject group = value.getAsJsonObject();
            for (int index = 0; index < model.bones.size(); index++) if (model.bones.get(index).name.equals(group.get("name").getAsString())) result.put(group.get("uuid").getAsString(), index);
        }
        return result;
    }
}
