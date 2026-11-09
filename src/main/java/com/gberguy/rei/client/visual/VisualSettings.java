package com.gberguy.rei.client.visual;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.common.config.Configuration;

public final class VisualSettings {
    public double height = 0.14;
    public double side = 0.55;
    public double behind = 0.10;
    public double vertical = -0.10;
    public double verticalClearance = 0.25;
    public double extraLift = 1.0;
    public double extraOutward = 1.5;
    public int distance = 48;
    public double transitionSeconds = 0.4;
    public double minDelay = 30;
    public double maxDelay = 90;
    public boolean particles = true;
    public boolean glow = true;
    public final Map<String, Double> weights = new LinkedHashMap<>();

    public static VisualSettings load(File file) {
        VisualSettings settings = new VisualSettings();
        Configuration config = new Configuration(file);
        config.load();
        String category = "visuals";
        settings.height = config.getFloat("heightMeters", category, 0.14f, 0.05f, 0.5f, "");
        settings.side = config.getFloat("shoulderSideOffset", category, 0.55f, -2, 2, "");
        settings.behind = config.getFloat("shoulderBackOffset", category, 0.10f, -2, 2, "");
        settings.vertical = config.getFloat("offsetFromEyes", category, -0.10f, -2, 2, "");
        settings.verticalClearance = config.getFloat("verticalClearanceInHeights", category, 0.25f, 0, 3, "");
        settings.extraLift = config.getFloat("extraLiftInHeights", category, 1.0f, -3, 3, "");
        settings.extraOutward = config.getFloat("extraOutwardInHeights", category, 1.5f, 0, 5, "");
        settings.distance = config.getInt("renderDistance", category, 48, 4, 128, "");
        settings.transitionSeconds = config.getFloat("transitionTicks", category, 8, 1, 100, "") / 20.0;
        settings.minDelay = config.getFloat("minimumIdleSeconds", category, 30, 1, 3600, "");
        settings.maxDelay = Math.max(settings.minDelay, config.getFloat("maximumIdleSeconds", category, 90, 1, 3600, ""));
        settings.particles = config.getBoolean("particles", category, true, "");
        settings.glow = config.getBoolean("glowMask", category, true, "");
        for (String entry : config.getStringList("animationWeights", category, new String[0], "")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                try {
                    double value = Double.parseDouble(parts[1].trim());
                    if (Double.isFinite(value) && value > 0 && value <= 1000000) settings.weights.put(parts[0].trim(), value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (config.hasChanged()) config.save();
        return settings;
    }
}
