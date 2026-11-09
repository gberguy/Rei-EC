package com.gberguy.rei.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public final class ModelAnimation {
    public final String name;
    public final double length;
    public final boolean loop;
    private final List<Track> tracks = new ArrayList<>();

    public ModelAnimation(JsonObject json, java.util.Map<String, Integer> bones) {
        name = json.get("name").getAsString();
        for (String property : new String[] {"anim_time_update", "blend_weight", "start_delay", "loop_delay"}) {
            if (json.has(property) && !json.get(property).getAsString().trim().isEmpty()) {
                throw new IllegalArgumentException("Unsupported animation property: " + property);
            }
        }
        length = json.get("length").getAsDouble();
        if (!Double.isFinite(length) || length <= 0 || length > 3600) throw new IllegalArgumentException("Invalid animation length");
        loop = "loop".equals(json.get("loop").getAsString());
        for (java.util.Map.Entry<String, JsonElement> entry : json.getAsJsonObject("animators").entrySet()) {
            JsonObject animator = entry.getValue().getAsJsonObject();
            JsonArray keys = animator.getAsJsonArray("keyframes");
            if (keys == null || keys.size() == 0) continue;
            Integer bone = bones.get(entry.getKey());
            if (bone == null || BlockbenchModel.flag(animator, "rotation_global", false)) {
                throw new IllegalArgumentException("Unsupported or missing animation bone: " + entry.getKey());
            }
            for (String channel : new String[] {"position", "rotation", "scale"}) {
                Track track = new Track(bone, channel.equals("position") ? 0 : channel.equals("rotation") ? 3 : 6);
                for (JsonElement key : keys) {
                    JsonObject value = key.getAsJsonObject();
                    String keyChannel = value.get("channel").getAsString();
                    if (!keyChannel.equals("position") && !keyChannel.equals("rotation") && !keyChannel.equals("scale")) {
                        throw new IllegalArgumentException("Unsupported animation channel: " + keyChannel);
                    }
                    if (channel.equals(keyChannel)) track.keys.add(new Key(value));
                }
                track.keys.sort(Comparator.comparingDouble(key -> key.time));
                if (!track.keys.isEmpty()) tracks.add(track);
            }
        }
    }

    public double[][] pose(int bones, double elapsed) {
        return pose(bones, elapsed, loop);
    }

    public double[][] pose(int bones, double elapsed, boolean repeat) {
        double time = repeat ? Math.max(0, elapsed) % length : Math.min(length, Math.max(0, elapsed));
        double[][] result = new double[bones][9];
        for (double[] bone : result) for (int axis = 6; axis < 9; axis++) bone[axis] = 1;
        for (Track track : tracks) {
            for (int axis = 0; axis < 3; axis++) result[track.bone][track.offset + axis] = track.sample(time, axis);
        }
        return result;
    }

    private static final class Track {
        final int bone;
        final int offset;
        final List<Key> keys = new ArrayList<>();

        Track(int bone, int offset) { this.bone = bone; this.offset = offset; }

        double sample(double time, int axis) {
            Key before = keys.get(0);
            for (Key after : keys) {
                if (time <= before.time) return before.value(time, axis, false);
                if (after.time > time) {
                    double a = before.value(time, axis, true);
                    if (before.mode.equals("step")) return a;
                    double b = after.value(time, axis, false);
                    double t = (time - before.time) / (after.time - before.time);
                    if (before.mode.equals("bezier") || after.mode.equals("bezier")) {
                        double gap = after.time - before.time;
                        double x1 = before.time + Math.max(0, Math.min(gap, before.rightTime[axis]));
                        double x2 = after.time + Math.max(-gap, Math.min(0, after.leftTime[axis]));
                        double low = 0, high = 1;
                        for (int step = 0; step < 24; step++) {
                            t = (low + high) / 2;
                            if (cubic(before.time, x1, x2, after.time, t) < time) low = t;
                            else high = t;
                        }
                        return cubic(a, a + before.rightValue[axis], b + after.leftValue[axis], b, t);
                    }
                    return a + (b - a) * t;
                }
                before = after;
            }
            return before.value(time, axis, true);
        }
    }

    private static double cubic(double a, double b, double c, double d, double t) {
        double s = 1 - t;
        return s * s * s * a + 3 * s * s * t * b + 3 * s * t * t * c + t * t * t * d;
    }

    private static final class Key {
        final double time;
        final String mode;
        final DoubleUnaryOperator[][] values;
        final double[] leftTime, rightTime, leftValue, rightValue;

        Key(JsonObject key) {
            time = key.get("time").getAsDouble();
            mode = key.has("interpolation") ? key.get("interpolation").getAsString() : "linear";
            if (!mode.equals("linear") && !mode.equals("bezier") && !mode.equals("step")) {
                throw new IllegalArgumentException("Unsupported keyframe interpolation: " + mode);
            }
            JsonArray points = key.getAsJsonArray("data_points");
            values = new DoubleUnaryOperator[points.size()][3];
            if (points.size() < 1 || points.size() > 2) throw new IllegalArgumentException("Unsupported animation data points");
            for (int point = 0; point < points.size(); point++) {
                JsonObject value = points.get(point).getAsJsonObject();
                int axis = 0;
                for (String name : new String[] {"x", "y", "z"}) values[point][axis++] = ModelExpression.compile(value.get(name).getAsString());
            }
            leftTime = BlockbenchModel.vector(key, "bezier_left_time", 0);
            rightTime = BlockbenchModel.vector(key, "bezier_right_time", 0);
            leftValue = BlockbenchModel.vector(key, "bezier_left_value", 0);
            rightValue = BlockbenchModel.vector(key, "bezier_right_value", 0);
        }

        double value(double time, int axis, boolean outgoing) {
            return values[outgoing ? values.length - 1 : 0][axis].applyAsDouble(time);
        }
    }
}
