package com.gberguy.rei.player;

import com.gberguy.rei.TestPlayers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class FeedingDataTest {
    @Test
    public void progressionIsClampedWithoutOverflow() {
        EntityPlayerMP player = TestPlayers.player();
        assertEquals(0, FeedingData.get(player));
        assertEquals(127, FeedingData.set(player, Long.MAX_VALUE));
        assertEquals(0, FeedingData.set(player, Long.MIN_VALUE));
        assertEquals(120, FeedingData.set(player, 120));
    }

    @Test
    public void playerDataSurvivesSerializationAndDoesNotChangeOtherData() throws Exception {
        EntityPlayerMP player = TestPlayers.player();
        NBTTagCompound other = new NBTTagCompound();
        other.setString("other_mod", "preserved");
        player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, other);
        FeedingData.set(player, 120);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(player.getEntityData(), output);
        NBTTagCompound restored = CompressedStreamTools.readCompressed(new ByteArrayInputStream(output.toByteArray()));
        player.getEntityData().merge(restored);
        assertEquals(120, FeedingData.get(player));
        assertEquals("preserved", player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getString("other_mod"));
    }

    @Test
    public void deathAndNonDeathClonesRetainOnlyTheirOwnPlayersProgression() {
        EntityPlayerMP original = TestPlayers.player();
        EntityPlayerMP unrelated = TestPlayers.player();
        FeedingData.set(original, 120);
        FeedingData.set(unrelated, 7);
        for (boolean death : new boolean[] {true, false}) {
            EntityPlayerMP clone = TestPlayers.player();
            PlayerLifecycle.clonePlayer(new PlayerEvent.Clone(clone, original, death));
            assertEquals(120, FeedingData.get(clone));
            FeedingData.set(clone, 10);
            assertEquals(120, FeedingData.get(original));
            assertEquals(7, FeedingData.get(unrelated));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientCannotChangeProgression() {
        EntityPlayerMP player = TestPlayers.player();
        TestPlayers.makeClient(player);
        FeedingData.set(player, 20);
    }
}
