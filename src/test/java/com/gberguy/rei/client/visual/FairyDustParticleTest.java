package com.gberguy.rei.client.visual;

import java.lang.reflect.Field;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FairyDustParticleTest {
    @Test
    public void renderedQuadIsTwoPercentOfTheCompanionHeightAndScalesWithIt() throws Exception {
        World world = mock(World.class);
        for (double height : new double[] {0.05, 0.14, 0.20, 0.50}) {
            Particle dust = new FairyDustParticle(world, 0, 0, 0, height);
            assertEquals(height * 0.02, field(dust, "particleScale") * 0.2, 1e-8);
            assertEquals(-height * 0.01, field(dust, "motionY"), 1e-9);
            assertFalse(dust.shouldDisableDepth());
        }
    }

    @Test
    public void dustFallsGentlyAndExpiresWithoutCollisionOrScaleGrowth() throws Exception {
        double height = 0.14;
        Particle dust = new FairyDustParticle(mock(World.class), 0, 10, 0, height);
        assertEquals(60, field(dust, "particleMaxAge"), 0);
        for (int tick = 0; tick < 60; tick++) {
            assertTrue(field(dust, "particleScale") * 0.2 < height * 0.021);
            dust.onUpdate();
            assertTrue(dust.isAlive());
        }
        dust.onUpdate();
        double drop = 10 - field(dust, "posY");
        assertTrue(drop > 0);
        assertTrue(drop < height * 0.5);
        assertFalse(dust.isAlive());
        assertEquals(0, field(dust, "posX"), 1e-9);
        assertEquals(0, field(dust, "posZ"), 1e-9);
    }

    private static double field(Particle particle, String name) throws Exception {
        Field field = Particle.class.getDeclaredField(name);
        field.setAccessible(true);
        return ((Number) field.get(particle)).doubleValue();
    }
}
