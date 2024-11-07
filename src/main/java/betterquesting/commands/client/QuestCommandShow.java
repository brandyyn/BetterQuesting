package betterquesting.commands.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.UuidConverter;
import betterquesting.api2.cache.QuestCache;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuestLines;
import betterquesting.commands.QuestCommandBase;
import betterquesting.questing.QuestDatabase;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class QuestCommandShow extends QuestCommandBase {

    public static boolean sentViaClick = false;
    private static UUID questId = null;

    @SubscribeEvent
    public void onOpenGui(GuiOpenEvent event) {
        if (questId != null) {
            event.gui = new GuiQuest(new GuiQuestLines(null), questId);
            MinecraftForge.EVENT_BUS.unregister(this);
            questId = null;
        }
    }

    @Override
    public String getCommand() {
        return "show";
    }

    @Override
    public void runCommand(MinecraftServer server, CommandBase command, ICommandSender sender, String[] args)
        throws CommandException {
        if (sender instanceof EntityPlayerSP && args.length == 2) {
            try {
                questId = UuidConverter.decodeUuid(args[1]);
                if (sentViaClick) {
                    sentViaClick = false;
                    Minecraft.getMinecraft()
                        .displayGuiScreen(new GuiQuest(new GuiQuestLines(null), questId));
                } else {
                    IQuest quest = QuestDatabase.INSTANCE.get(questId);
                    if (quest != null) {
                        EntityPlayerSP player = (EntityPlayerSP) sender;
                        if (QuestCache.isQuestShown(quest, QuestingAPI.getQuestingUUID(player), player)) {
                            MinecraftForge.EVENT_BUS.register(this);
                            return;
                        } else {
                            sender.addChatMessage(
                                new ChatComponentTranslation("betterquesting.msg.share_quest_hover_text_failure"));
                        }
                    }
                    sender.addChatMessage(
                        new ChatComponentTranslation(
                            "betterquesting.msg.share_quest_invalid",
                            String.valueOf(questId)));
                }
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentTranslation("betterquesting.msg.share_quest_invalid", args[1]));
            }
        }
    }

    @Override
    public String getUsageSuffix() {
        return "[<quest_id>]";
    }

    @Override
    public boolean validArgs(String[] args) {
        return args.length == 2;
    }

    @Override
    public List<String> autoComplete(MinecraftServer server, ICommandSender sender, String[] args) {
        return args.length == 2 ? QuestDatabase.INSTANCE.keySet()
            .stream()
            .map(UuidConverter::encodeUuid)
            .collect(Collectors.toList()) : Collections.emptyList();
    }
}
