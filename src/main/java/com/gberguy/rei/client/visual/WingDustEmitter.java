package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WingDustEmitter {
    private final BlockbenchModel model;
    private final int[] wingCubes = {-1, -1};
    private final boolean[] outwardPositive = new boolean[2];

    public WingDustEmitter(BlockbenchModel model) {
        this.model = model;
        for (int wing = 0; wing < 2; wing++) {
            String name = wing == 0 ? "left_wing" : "right_wing";
            double largest = -1;
            for (int index = 0; index < model.cubes.size(); index++) {
                BlockbenchModel.Cube cube = model.cubes.get(index);
                int bone = cube.bone;
                while (bone >= 0 && !model.bones.get(bone).name.equals(name)) bone = model.bones.get(bone).parent;
                if (bone < 0) continue;
                double x = Math.abs(cube.to[0] - cube.from[0]);
                double y = Math.abs(cube.to[1] - cube.from[1]);
                double z = Math.abs(cube.to[2] - cube.from[2]);
                double area = Math.max(x * y, Math.max(x * z, y * z));
                if (area > largest) {
                    largest = area;
                    wingCubes[wing] = index;
                    outwardPositive[wing] = (cube.from[0] + cube.to[0]) / 2 >= model.bones.get(bone).origin[0];
                }
            }
        }
    }

    public double[][] emissions(double[][] pose, double[] anchor, double yaw, double height, int particleSetting, Random random) {
        if (particleSetting >= 2) return new double[0][];
        List<double[]> result = new ArrayList<>();
        for (double[] site : points(pose, anchor, yaw, height, random)) {
            if (random.nextInt(particleSetting == 1 ? 8 : 4) == 0) result.add(site);
        }
        return result.toArray(new double[result.size()][]);
    }

    public double[][] points(double[][] pose, double[] anchor, double yaw, double height, Random random) {
        List<double[]> points = new ArrayList<>();
        double[][][] vertices = model.vertices(pose);
        double scale = height / model.height;
        double angle = Math.toRadians(180 - yaw);
        double cosine = Math.cos(angle), sine = Math.sin(angle);
        for (int wing = 0; wing < 2; wing++) {
            if (wingCubes[wing] < 0) continue;
            for (int site = 0; site < 4; site++) {
                double fraction = (site + 1) / 5.0 + (random.nextDouble() - 0.5) * 0.05;
                double x = outwardPositive[wing] ? fraction : 1 - fraction;
                double y = 0.75 + (random.nextDouble() - 0.5) * 0.15;
                double[] position = new double[3];
                for (int corner = 0; corner < 8; corner++) {
                    double weight = ((corner & 1) == 0 ? 1 - x : x) * ((corner & 2) == 0 ? 1 - y : y) * 0.5;
                    for (int axis = 0; axis < 3; axis++) position[axis] += vertices[wingCubes[wing]][corner][axis] * weight;
                }
                for (int axis = 0; axis < 3; axis++) position[axis] = (position[axis] - model.center[axis]) * scale;
                position[2] += height * 0.02;
                points.add(new double[] {anchor[0] + cosine * position[0] + sine * position[2],
                        anchor[1] + position[1], anchor[2] - sine * position[0] + cosine * position[2]});
            }
        }
        return points.toArray(new double[points.size()][]);
    }
}
