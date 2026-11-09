package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import com.gberguy.rei.client.model.BlockbenchModelTest;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompanionTexturesTest {
    @Test
    public void startupDefersUploadUntilTheBackendExistsAndReusesTexturesAcrossFrames() throws Exception {
        BlockbenchModel model = BlockbenchModelTest.model();
        CompanionTextures textures = new CompanionTextures();
        Backend backend = new Backend();
        assertFalse(textures.prepare(model, null));
        assertEquals(0, textures.size());
        assertTrue(textures.prepare(model, backend));
        assertEquals(model.textures, backend.images);
        assertEquals(model.textures.size(), textures.size());
        assertTrue(textures.prepare(model, backend));
        assertEquals(model.textures.size(), backend.images.size());
        assertTrue(backend.deleted.isEmpty());
    }

    @Test
    public void resourceReloadReplacesOldTexturesAndAnInvalidModelReleasesThem() throws Exception {
        CompanionTextures textures = new CompanionTextures();
        Backend backend = new Backend();
        BlockbenchModel original = BlockbenchModelTest.model();
        assertTrue(textures.prepare(original, backend));
        ResourceLocation first = textures.get(0);
        BlockbenchModel replacement = BlockbenchModelTest.model();
        assertTrue(textures.prepare(replacement, backend));
        assertEquals(original.textures.size(), backend.deleted.size());
        assertTrue(backend.deleted.contains(first));
        assertEquals(original.textures.size() + replacement.textures.size(), backend.images.size());
        assertFalse(textures.prepare(null, backend));
        assertEquals(0, textures.size());
        assertEquals(backend.images.size(), backend.deleted.size());
    }

    @Test
    public void textureManagerReplacementDeletesTexturesThroughTheirOriginalOwner() throws Exception {
        CompanionTextures textures = new CompanionTextures();
        BlockbenchModel model = BlockbenchModelTest.model();
        Backend original = new Backend();
        Backend replacement = new Backend();
        assertTrue(textures.prepare(model, original));
        assertTrue(textures.prepare(model, replacement));
        assertEquals(model.textures.size(), original.deleted.size());
        assertEquals(model.textures.size(), replacement.images.size());
        assertTrue(replacement.deleted.isEmpty());
    }

    @Test
    public void partialUploadFailureDoesNotLeaveRegisteredTexturesBehind() throws Exception {
        CompanionTextures textures = new CompanionTextures();
        BlockbenchModel model = BlockbenchModelTest.model();
        Backend broken = new Backend() {
            @Override
            public ResourceLocation upload(int index, BufferedImage image) {
                if (index == 1) throw new IllegalStateException("Upload failed");
                return super.upload(index, image);
            }
        };
        try {
            textures.prepare(model, broken);
            fail("Expected the failed upload to be reported");
        } catch (IllegalStateException expected) {
            assertEquals("Upload failed", expected.getMessage());
        }
        assertEquals(0, textures.size());
        assertEquals(1, broken.deleted.size());
        assertFalse(textures.matches(model, broken));
        assertTrue(textures.prepare(model, new Backend()));
    }

    private static class Backend implements CompanionTextures.Backend {
        final List<BufferedImage> images = new ArrayList<>();
        final List<ResourceLocation> deleted = new ArrayList<>();

        @Override
        public ResourceLocation upload(int index, BufferedImage image) {
            images.add(image);
            return new ResourceLocation("rei", "test/" + images.size());
        }

        @Override
        public void delete(ResourceLocation location) {
            deleted.add(location);
        }
    }
}
