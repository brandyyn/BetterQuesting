package betterquesting.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.commands.QuestCommandBase;
import betterquesting.questing.QuestDatabase;
import betterquesting.storage.NameCache;

public class QuestCommandReportAllProgress extends QuestCommandBase {

    @Override
    public String getUsageSuffix() {
        return "<username|uuid>";
    }

    @Override
    public boolean validArgs(String[] args) {
        return args.length == 2;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> autoComplete(MinecraftServer server, ICommandSender sender, String[] args) {
        ArrayList<String> list = new ArrayList<>();

        if (args.length == 2) {
            return CommandBase.getListOfStringsMatchingLastWord(
                args,
                NameCache.INSTANCE.getAllNames()
                    .toArray(new String[0]));
        }

        return list;
    }

    @Override
    public String getCommand() {
        return "check_all";
    }

    @Override
    public void runCommand(MinecraftServer server, CommandBase command, ICommandSender sender, String[] args)
        throws CommandException {
        UUID uuid;

        uuid = this.findPlayerID(server, sender, args[1]);

        if (uuid == null) {
            throw this.getException(command);
        }

        sender.addChatMessage(
            new ChatComponentTranslation("betterquesting.cmd.check_all", NameCache.INSTANCE.getName(uuid)));

        for (Map.Entry<UUID, IQuest> entry : QuestDatabase.INSTANCE.entrySet()) {
            if (entry.getValue()
                .isComplete(uuid)) {
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "betterquesting.cmd.check_all.line",
                        entry.getKey(),
                        entry.getValue()
                            .getProperty(NativeProps.NAME)));
            }
        }
    }

    @Override
    public boolean isArgUsername(String[] args, int index) {
        return index == 1;
    }
}
