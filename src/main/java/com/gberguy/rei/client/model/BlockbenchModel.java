package com.gberguy.rei.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public final class BlockbenchModel {
    public static final String IDLE = "animation.succubus.idle";
    public final List<Bone> bones = new ArrayList<>();
    public final List<Cube> cubes = new ArrayList<>();
    public final List<BufferedImage> textures = new ArrayList<>();
    public final List<String> textureNames = new ArrayList<>();
    public final Map<String, ModelAnimation> animations = new LinkedHashMap<>();
    public final double textureWidth;
    public final double textureHeight;
    public final double[] center = new double[3];
    public final double height;
    private final Map<String, Integer> boneIds = new HashMap<>();

    public static BlockbenchModel read(Reader reader) throws IOException {
        return new BlockbenchModel(new JsonParser().parse(reader).getAsJsonObject());
    }

    private BlockbenchModel(JsonObject source) throws IOException {
        String format = source.getAsJsonObject("meta").get("model_format").getAsString();
        if (!format.equals("free")) throw new IllegalArgumentException("Rei requires a free-format cube Blockbench model");
        JsonObject resolution = source.getAsJsonObject("resolution");
        textureWidth = resolution.get("width").getAsDouble();
        textureHeight = resolution.get("height").getAsDouble();
        if (textureWidth <= 0 || textureHeight <= 0) throw new IllegalArgumentException("Invalid texture resolution");
        for (JsonElement value : source.getAsJsonArray("textures")) {
            JsonObject texture = value.getAsJsonObject();
            String uri = texture.get("source").getAsString();
            if (!uri.startsWith("data:image/png;base64,")) throw new IllegalArgumentException("Model textures must be embedded PNGs");
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1))));
            if (image == null || image.getWidth() > 2048 || image.getHeight() > 2048) throw new IllegalArgumentException("Invalid embedded texture");
            textures.add(image);
            textureNames.add(texture.get("name").getAsString());
        }
        if (textures.isEmpty()) throw new IllegalArgumentException("No model textures");
        Map<String, JsonObject> groups = new HashMap<>();
        if (source.has("groups")) for (JsonElement value : source.getAsJsonArray("groups")) {
            JsonObject group = value.getAsJsonObject();
            groups.put(group.get("uuid").getAsString(), group);
        }
        Map<String, JsonObject> elements = new HashMap<>();
        for (JsonElement value : source.getAsJsonArray("elements")) {
            JsonObject element = value.getAsJsonObject();
            if (!element.get("type").getAsString().equals("cube")) throw new IllegalArgumentException("Only cube model elements are supported");
            elements.put(element.get("uuid").getAsString(), element);
        }
        Set<String> visited = new HashSet<>();
        for (JsonElement node : source.getAsJsonArray("outliner")) loadNode(node, -1, true, groups, elements, visited);
        if (visited.size() != groups.size() + elements.size()) throw new IllegalArgumentException("Unreferenced or duplicate model nodes");
        for (JsonElement value : source.getAsJsonArray("animations")) {
            ModelAnimation animation = new ModelAnimation(value.getAsJsonObject(), boneIds);
            if (animations.put(animation.name, animation) != null) throw new IllegalArgumentException("Duplicate animation name");
        }
        if (!animations.containsKey(IDLE)) throw new IllegalArgumentException("Missing default animation: " + IDLE);
        if (cubes.isEmpty()) throw new IllegalArgumentException("No exported model geometry");
        double[][][] first = vertices(animations.get(IDLE).pose(bones.size(), 0));
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double[][] cube : first) for (double[] vertex : cube) for (int axis = 0; axis < 3; axis++) {
            min[axis] = Math.min(min[axis], vertex[axis]);
            max[axis] = Math.max(max[axis], vertex[axis]);
        }
        for (int axis = 0; axis < 3; axis++) center[axis] = (min[axis] + max[axis]) / 2;
        height = max[1] - min[1];
        if (height < 0.001) throw new IllegalArgumentException("Empty model bounds");
        for (ModelAnimation animation : animations.values()) {
            for (int sample = 0; sample <= Math.ceil(animation.length * 20); sample++) {
                animation.pose(bones.size(), sample / 20.0);
            }
        }
    }

    private void loadNode(JsonElement node, int parent, boolean visible, Map<String, JsonObject> groups,
                          Map<String, JsonObject> elements, Set<String> visited) {
        String id = node.isJsonPrimitive() ? node.getAsString() : node.getAsJsonObject().get("uuid").getAsString();
        if (!visited.add(id)) throw new IllegalArgumentException("Repeated model node: " + id);
        if (elements.containsKey(id)) {
            JsonObject element = elements.get(id);
            if (visible && flag(element, "export", true) && flag(element, "visibility", true)) cubes.add(new Cube(element, parent, textures.size()));
            return;
        }
        JsonObject group = groups.get(id);
        if (group == null && node.isJsonObject() && node.getAsJsonObject().has("name")) group = node.getAsJsonObject();
        if (group == null) throw new IllegalArgumentException("Missing bone: " + id);
        Bone bone = new Bone(group, parent);
        int index = bones.size();
        bones.add(bone);
        boneIds.put(id, index);
        JsonArray children = node.isJsonObject() ? node.getAsJsonObject().getAsJsonArray("children") : group.getAsJsonArray("children");
        if (children != null) for (JsonElement child : children) loadNode(child, index,
                visible && flag(group, "export", true) && flag(group, "visibility", true), groups, elements, visited);
    }

    public double[][][] vertices(double[][] pose) {
        Matrix4d[] transforms = new Matrix4d[bones.size()];
        for (int index = 0; index < bones.size(); index++) {
            Bone bone = bones.get(index);
            Matrix4d matrix = identity();
            if (bone.parent >= 0) matrix.set(transforms[bone.parent]);
            translate(matrix, pose[index][0] + bone.origin[0], pose[index][1] + bone.origin[1], pose[index][2] + bone.origin[2]);
            rotate(matrix, bone.rotation[0] + pose[index][3], bone.rotation[1] + pose[index][4], bone.rotation[2] + pose[index][5]);
            Matrix4d scale = identity();
            scale.m00 = pose[index][6]; scale.m11 = pose[index][7]; scale.m22 = pose[index][8];
            matrix.mul(scale);
            translate(matrix, -bone.origin[0], -bone.origin[1], -bone.origin[2]);
            transforms[index] = matrix;
        }
        double[][][] result = new double[cubes.size()][8][3];
        for (int index = 0; index < cubes.size(); index++) {
            Cube cube = cubes.get(index);
            Matrix4d matrix = identity();
            if (cube.bone >= 0) matrix.set(transforms[cube.bone]);
            translate(matrix, cube.origin[0], cube.origin[1], cube.origin[2]);
            rotate(matrix, cube.rotation[0], cube.rotation[1], cube.rotation[2]);
            if (cube.rescale) {
                Matrix4d scale = identity();
                double x = 1 / Math.cos(Math.toRadians(cube.rotation[0]));
                double y = 1 / Math.cos(Math.toRadians(cube.rotation[1]));
                double z = 1 / Math.cos(Math.toRadians(cube.rotation[2]));
                scale.m00 = y * z; scale.m11 = x * z; scale.m22 = x * y;
                matrix.mul(scale);
            }
            translate(matrix, -cube.origin[0], -cube.origin[1], -cube.origin[2]);
            for (int vertex = 0; vertex < 8; vertex++) {
                Point3d point = new Point3d((vertex & 1) == 0 ? cube.from[0] : cube.to[0],
                        (vertex & 2) == 0 ? cube.from[1] : cube.to[1], (vertex & 4) == 0 ? cube.from[2] : cube.to[2]);
                matrix.transform(point);
                result[index][vertex] = new double[] {point.x, point.y, point.z};
            }
        }
        return result;
    }

    private static Matrix4d identity() {
        Matrix4d value = new Matrix4d(); value.setIdentity(); return value;
    }

    private static void translate(Matrix4d matrix, double x, double y, double z) {
        Matrix4d value = identity(); value.setTranslation(new Vector3d(x, y, z)); matrix.mul(value);
    }

    private static void rotate(Matrix4d matrix, double x, double y, double z) {
        Matrix4d value = new Matrix4d();
        value.rotZ(Math.toRadians(z)); matrix.mul(value);
        value.rotY(Math.toRadians(y)); matrix.mul(value);
        value.rotX(Math.toRadians(x)); matrix.mul(value);
    }

    public static double[] vector(JsonObject json, String name, double fallback) {
        double[] result = {fallback, fallback, fallback};
        if (json.has(name)) {
            JsonArray values = json.getAsJsonArray(name);
            if (values.size() != 3) throw new IllegalArgumentException("Invalid vector: " + name);
            for (int axis = 0; axis < 3; axis++) {
                result[axis] = values.get(axis).getAsDouble();
                if (!Double.isFinite(result[axis])) throw new IllegalArgumentException("Non-finite vector: " + name);
            }
        }
        return result;
    }

    public static boolean flag(JsonObject json, String name, boolean fallback) {
        return json.has(name) ? json.get(name).getAsBoolean() : fallback;
    }

    public static final class Bone {
        public final String name;
        public final int parent;
        public final double[] origin, rotation;
        Bone(JsonObject json, int parent) {
            name = json.get("name").getAsString(); this.parent = parent;
            origin = vector(json, "origin", 0); rotation = vector(json, "rotation", 0);
            if (flag(json, "reset", false)) throw new IllegalArgumentException("Reset bones are not supported");
        }
    }

    public static final class Cube {
        public final int bone;
        public final double[] origin, rotation, from, to;
        public final boolean rescale;
        public final List<Face> faces = new ArrayList<>();
        Cube(JsonObject json, int bone, int textures) {
            this.bone = bone; origin = vector(json, "origin", 0); rotation = vector(json, "rotation", 0);
            from = vector(json, "from", 0); to = vector(json, "to", 0);
            rescale = flag(json, "rescale", false);
            double inflate = json.has("inflate") ? json.get("inflate").getAsDouble() : 0;
            for (int axis = 0; axis < 3; axis++) { from[axis] -= inflate; to[axis] += inflate; }
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("faces").entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                if (!face.has("texture") || face.get("texture").isJsonNull() || face.get("texture").getAsInt() < 0) continue;
                Face parsed = new Face(entry.getKey(), face);
                if (parsed.texture >= textures) throw new IllegalArgumentException("Missing face texture");
                faces.add(parsed);
            }
        }
    }

    public static final class Face {
        public final int[] corners;
        public final int texture;
        public final double[] uv = new double[4];
        public final int rotation;
        Face(String name, JsonObject json) {
            switch (name) {
                case "north": corners = new int[] {3, 1, 0, 2}; break;
                case "south": corners = new int[] {6, 4, 5, 7}; break;
                case "east": corners = new int[] {7, 5, 1, 3}; break;
                case "west": corners = new int[] {2, 0, 4, 6}; break;
                case "up": corners = new int[] {6, 7, 3, 2}; break;
                case "down": corners = new int[] {0, 1, 5, 4}; break;
                default: throw new IllegalArgumentException("Unknown cube face: " + name);
            }
            texture = json.get("texture").getAsInt();
            for (int axis = 0; axis < 4; axis++) uv[axis] = json.getAsJsonArray("uv").get(axis).getAsDouble();
            rotation = json.has("rotation") ? json.get("rotation").getAsInt() : 0;
            if (rotation % 90 != 0) throw new IllegalArgumentException("Unsupported face rotation");
        }
    }
}
