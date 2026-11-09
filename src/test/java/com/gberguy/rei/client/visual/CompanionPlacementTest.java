package com.gberguy.rei.client.visual;

import com.gberguy.rei.TestPlayers;
import net.minecraft.entity.player.EntityPlayerMP;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CompanionPlacementTest {
    @Test
    public void anchorFollowsInterpolatedPlayerWithoutAccumulatedPositionState() {
        EntityPlayerMP player = TestPlayers.player();
        player.lastTickPosX = 10; player.posX = 12;
        player.lastTickPosY = 20; player.posY = 22;
        player.lastTickPosZ = 30; player.posZ = 32;
        when(player.getEyeHeight()).thenReturn(1.6f);
        VisualSettings settings = new VisualSettings();
        double side = settings.side + settings.height * settings.extraOutward;
        assertArrayEquals(new double[] {11 - side, 21 + 1.6 + settings.vertical + settings.height * (settings.verticalClearance + settings.extraLift), 31 - settings.behind},
                CompanionVisuals.anchor(player, 0.5f, settings), 1e-6);
        player.lastTickPosX = player.posX = 10000;
        assertEquals(10000 - side, CompanionVisuals.anchor(player, 1, settings)[0], 1e-6);
    }

    @Test
    public void addedClearanceRaisesTheModelByOneQuarterOfItsHeight() {
        EntityPlayerMP player = TestPlayers.player();
        VisualSettings settings = new VisualSettings();
        settings.extraLift = 0;
        double raised = CompanionVisuals.anchor(player, 1, settings)[1];
        settings.verticalClearance = 0;
        double original = CompanionVisuals.anchor(player, 1, settings)[1];
        assertEquals(0.035, raised - original, 1e-9);
        settings.height = 0.20;
        settings.verticalClearance = 0.25;
        assertEquals(original + 0.05, CompanionVisuals.anchor(player, 1, settings)[1], 1e-9);
    }

    @Test
    public void additionalOffsetsMoveUpOneReiAndOutwardOneAndAHalfReiFromThePreviousBuild() {
        EntityPlayerMP player = TestPlayers.player();
        VisualSettings settings = new VisualSettings();
        double[] next = CompanionVisuals.anchor(player, 1, settings);
        settings.extraLift = 0; settings.extraOutward = 0;
        double[] previous = CompanionVisuals.anchor(player, 1, settings);
        assertEquals(0.14, next[1] - previous[1], 1e-9);
        assertEquals(-0.21, next[0] - previous[0], 1e-9);
        assertEquals(previous[2], next[2], 1e-9);
        settings.side = -0.55;
        previous = CompanionVisuals.anchor(player, 1, settings);
        settings.extraLift = 1; settings.extraOutward = 1.5;
        next = CompanionVisuals.anchor(player, 1, settings);
        assertEquals(0.21, next[0] - previous[0], 1e-9);
        assertEquals(0.14, next[1] - previous[1], 1e-9);
    }

    @Test
    public void bodyYawWrapsAcross180WithoutSpinningTheOffset() {
        EntityPlayerMP player = TestPlayers.player();
        player.prevRenderYawOffset = 179; player.renderYawOffset = -179;
        VisualSettings settings = new VisualSettings();
        double[] anchor = CompanionVisuals.anchor(player, 0.5f, settings);
        assertEquals(settings.side + settings.height * settings.extraOutward, anchor[0], 1e-9);
        assertEquals(settings.behind, anchor[2], 1e-9);
    }
}
