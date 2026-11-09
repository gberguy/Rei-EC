package com.gberguy.rei.client.visual;

import com.gberguy.rei.client.model.BlockbenchModel;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CompanionRendererStartupTest {
    @Test
    public void immediateReloadBeforeTextureManagerExistsKeepsTheModelForLaterRendering() throws Exception {
        Field instance = Minecraft.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        Minecraft startup = mock(Minecraft.class);
        instance.set(null, startup);
        try {
            IResourceManager resources = mock(IResourceManager.class);
            IResource resource = mock(IResource.class);
            when(resources.getResource(CompanionRenderer.MODEL)).thenReturn(resource);
            when(resource.getInputStream()).thenAnswer(invocation -> getClass().getResourceAsStream("/assets/rei/companion/rei.bbmodel"));
            CompanionRenderer renderer = new CompanionRenderer();
            renderer.onResourceManagerReload(resources);
            BlockbenchModel first = renderer.model();
            assertNotNull(first);
            verifyZeroInteractions(startup);
            assertFalse(renderer.prepare());
            assertSame(first, renderer.model());
            renderer.onResourceManagerReload(resources);
            assertNotNull(renderer.model());
            assertNotSame(first, renderer.model());
            assertFalse(renderer.prepare());
            assertNotNull(renderer.model());
        } finally {
            instance.set(null, previous);
        }
    }
}
