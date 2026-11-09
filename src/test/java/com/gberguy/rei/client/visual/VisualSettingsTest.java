package com.gberguy.rei.client.visual;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import java.lang.reflect.Field;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class VisualSettingsTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private Field minecraftHome;
    private Object previousHome;

    @Before
    public void prepareForgeConfigEnvironment() throws Exception {
        minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
    }

    @After
    public void restoreForgeConfigEnvironment() throws Exception {
        minecraftHome.set(null, previousHome);
    }

    @Test
    public void existingClientConfigReceivesClearanceWithoutReplacingPlacementOrParticlePreferences() throws Exception {
        File file = temporary.newFile("rei-visuals.cfg");
        Configuration old = new Configuration(file);
        old.get("visuals", "heightMeters", 0.14).set(0.14);
        old.get("visuals", "offsetFromEyes", -0.10).set(-0.10);
        old.get("visuals", "shoulderSideOffset", 0.55).set(0.70);
        old.get("visuals", "verticalClearanceInHeights", 0.25).set(0.25);
        old.get("visuals", "particles", false).set(false);
        old.save();
        VisualSettings loaded = VisualSettings.load(file);
        assertEquals(0.25, loaded.verticalClearance, 1e-9);
        assertEquals(1.0, loaded.extraLift, 1e-9);
        assertEquals(1.5, loaded.extraOutward, 1e-9);
        assertEquals(-0.10, loaded.vertical, 1e-6);
        assertEquals(0.70, loaded.side, 1e-6);
        assertFalse(loaded.particles);
        VisualSettings again = VisualSettings.load(file);
        assertEquals(0.25, again.verticalClearance, 1e-9);
        assertEquals(1.0, again.extraLift, 1e-9);
        assertEquals(1.5, again.extraOutward, 1e-9);
    }
}
