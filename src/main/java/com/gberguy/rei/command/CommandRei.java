package com.gberguy.rei.command;

import com.gberguy.rei.network.ReiNetwork;
import com.gberguy.rei.player.FeedingData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public final class CommandRei extends CommandBase {
    private final Consumer<EntityPlayerMP> synchronize;

    public CommandRei() {
        this(ReiNetwork::sync);
    }

    CommandRei(Consumer<EntityPlayerMP> synchronize) {
        this.synchronize = synchronize;
    }

    @Override
    public String getName() {
        return "ecrei";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.rei.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }
        String operation = args[1];
        boolean query = "get".equals(operation);
        if ((!query && !Arrays.asList("set", "add", "sub").contains(operation))
                || args.length != (query ? 2 : 3)) {
            throw new WrongUsageException(getUsage(sender));
        }
        EntityPlayerMP target = getPlayer(server, sender, args[0]);
        int current = FeedingData.get(target);
        if (query) {
            sender.sendMessage(new TextComponentTranslation("commands.rei.value", target.getName(), current));
            return;
        }
        int amount = parseInt(args[2]);
        long value = "set".equals(operation) ? amount
                : "add".equals(operation) ? (long) current + amount : (long) current - amount;
        int result = FeedingData.set(target, value);
        synchronize.accept(target);
        notifyCommandListener(sender, this, "commands.rei.value", target.getName(), result);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                         @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "set", "add", "sub", "get");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}
