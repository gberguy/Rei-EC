package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ResourceLocation;

final class CompanionTextures {
    interface Backend {
        ResourceLocation upload(int index, BufferedImage image);
        void delete(ResourceLocation location);
    }

    private final List<ResourceLocation> locations = new ArrayList<>();
    private BlockbenchModel uploadedModel;
    private Backend owner;

    boolean matches(BlockbenchModel model, Backend backend) {
        return model != null && model == uploadedModel && owner == backend;
    }

    boolean prepare(BlockbenchModel model, Backend backend) {
        if (backend == null) return false;
        if (matches(model, backend)) return true;
        release();
        if (model == null) return false;
        owner = backend;
        try {
            for (int index = 0; index < model.textures.size(); index++) {
                locations.add(backend.upload(index, model.textures.get(index)));
            }
            uploadedModel = model;
            return true;
        } catch (RuntimeException exception) {
            release();
            throw exception;
        }
    }

    private void release() {
        List<ResourceLocation> previous = new ArrayList<>(locations);
        Backend previousOwner = owner;
        locations.clear(); uploadedModel = null; owner = null;
        if (previousOwner != null) for (ResourceLocation location : previous) previousOwner.delete(location);
    }

    int size() { return locations.size(); }
    ResourceLocation get(int index) { return locations.get(index); }
}
