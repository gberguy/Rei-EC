package com.gberguy.rei.command;

import com.gberguy.rei.TestPlayers;
import com.gberguy.rei.player.FeedingData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CommandReiTest {
    @Test
    public void commandsChangeTheOnlineTargetWithoutAnItemAndClampLargeArithmetic() throws Exception {
        EntityPlayerMP target = TestPlayers.player();
        EntityPlayerMP other = TestPlayers.player();
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getPlayerByUsername("target")).thenReturn(target);
        when(target.getName()).thenReturn("target");
        ICommandSender sender = mock(ICommandSender.class);
        List<EntityPlayerMP> synchronizedPlayers = new ArrayList<>();
        CommandRei command = new CommandRei(synchronizedPlayers::add);
        command.execute(server, sender, new String[] {"target", "set", "10"});
        assertEquals(10, FeedingData.get(target));
        command.execute(server, sender, new String[] {"target", "add", "17"});
        assertEquals(27, FeedingData.get(target));
        command.execute(server, sender, new String[] {"target", "sub", "12"});
        assertEquals(15, FeedingData.get(target));
        command.execute(server, sender, new String[] {"target", "get"});
        ArgumentCaptor<ITextComponent> feedback = ArgumentCaptor.forClass(ITextComponent.class);
        verify(sender).sendMessage(feedback.capture());
        assertArrayEquals(new Object[] {"target", 15},
                ((net.minecraft.util.text.TextComponentTranslation) feedback.getValue()).getFormatArgs());
        command.execute(server, sender, new String[] {"target", "add", "2147483647"});
        assertEquals(127, FeedingData.get(target));
        command.execute(server, sender, new String[] {"target", "sub", "2147483647"});
        assertEquals(0, FeedingData.get(target));
        command.execute(server, sender, new String[] {"target", "set", "-5"});
        assertEquals(0, FeedingData.get(target));
        command.execute(server, sender, new String[] {"target", "add", "17"});
        command.execute(server, sender, new String[] {"target", "add", "17"});
        assertEquals(34, FeedingData.get(target));
        assertEquals(0, FeedingData.get(other));
        assertEquals(8, synchronizedPlayers.size());
        assertTrue(synchronizedPlayers.stream().allMatch(player -> player == target));
    }

    @Test
    public void permissionIsOperatorOnly() {
        CommandRei command = new CommandRei(player -> { });
        ICommandSender sender = mock(ICommandSender.class);
        assertEquals(2, command.getRequiredPermissionLevel());
        assertFalse(command.checkPermission(null, sender));
        when(sender.canUseCommand(2, "ecrei")).thenReturn(true);
        assertTrue(command.checkPermission(null, sender));
    }

    @Test(expected = WrongUsageException.class)
    public void invalidOperationDoesNotTouchPlayerData() throws Exception {
        new CommandRei(player -> fail()).execute(null, null, new String[] {"target", "grant", "1"});
    }
}
