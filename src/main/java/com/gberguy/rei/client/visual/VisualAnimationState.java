package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import com.gberguy.rei.client.model.ModelAnimation;
import java.util.Map;
import java.util.Random;

public final class VisualAnimationState {
    private final Random random;
    private String active = BlockbenchModel.IDLE;
    private double start;
    private double nextChoice;
    private double transitionStart;
    private double[][] previous;

    public VisualAnimationState(long seed, double now, VisualSettings settings) {
        random = new Random(seed); start = now;
        schedule(now, settings);
    }

    public String active() { return active; }

    public double[][] pose(BlockbenchModel model, double now, VisualSettings settings) {
        ModelAnimation clip = model.animations.get(active);
        if (!active.equals(BlockbenchModel.IDLE) && now - start >= clip.length) {
            change(model, BlockbenchModel.IDLE, now, settings);
            schedule(now, settings);
        } else if (active.equals(BlockbenchModel.IDLE) && now >= nextChoice) {
            double total = 0;
            for (Map.Entry<String, Double> entry : settings.weights.entrySet()) {
                if (!entry.getKey().equals(BlockbenchModel.IDLE) && model.animations.containsKey(entry.getKey())) total += entry.getValue();
            }
            double selected = random.nextDouble() * total;
            for (Map.Entry<String, Double> entry : settings.weights.entrySet()) {
                if (entry.getKey().equals(BlockbenchModel.IDLE) || !model.animations.containsKey(entry.getKey())) continue;
                selected -= entry.getValue();
                if (selected < 0) { change(model, entry.getKey(), now, settings); break; }
            }
            schedule(now, settings);
        }
        return blended(model, now, settings);
    }

    private void change(BlockbenchModel model, String next, double now, VisualSettings settings) {
        previous = blended(model, now, settings);
        active = next; start = now; transitionStart = now;
    }

    private double[][] blended(BlockbenchModel model, double now, VisualSettings settings) {
        ModelAnimation clip = model.animations.get(active);
        double[][] result = clip.pose(model.bones.size(), now - start, active.equals(BlockbenchModel.IDLE) && clip.loop);
        if (previous != null) {
            double t = Math.max(0, Math.min(1, (now - transitionStart) / settings.transitionSeconds));
            t = t * t * (3 - 2 * t);
            for (int bone = 0; bone < result.length; bone++) for (int axis = 0; axis < 9; axis++) {
                double delta = result[bone][axis] - previous[bone][axis];
                if (axis >= 3 && axis < 6) delta -= Math.floor((delta + 180) / 360) * 360;
                result[bone][axis] = previous[bone][axis] + delta * t;
            }
            if (t >= 1) previous = null;
        }
        return result;
    }

    private void schedule(double now, VisualSettings settings) {
        nextChoice = now + settings.minDelay + random.nextDouble() * Math.max(0, settings.maxDelay - settings.minDelay);
    }
}
