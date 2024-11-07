package betterquesting.client.toolbox.tools;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.lwjgl.input.Keyboard;

import betterquesting.api.client.toolbox.IToolboxTool;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.utils.NBTConverter;
import betterquesting.api2.client.gui.controls.PanelButtonQuest;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestLine;
import betterquesting.client.gui2.editors.designer.PanelToolController;
import betterquesting.network.handlers.NetChapterEdit;
import betterquesting.questing.QuestLineDatabase;

public class ToolboxToolRemove implements IToolboxTool {

    private CanvasQuestLine gui;

    @Override
    public void initTool(CanvasQuestLine gui) {
        this.gui = gui;
    }

    @Override
    public void disableTool() {}

    @Override
    public void refresh(CanvasQuestLine gui) {}

    @Override
    public boolean onMouseClick(int mx, int my, int click) {
        if (click != 0 || !gui.getTransform()
            .contains(mx, my)) {
            return false;
        }

        IQuestLine line = gui.getQuestLine();
        PanelButtonQuest btn = gui.getButtonAt(mx, my);

        if (line != null && btn != null) {
            if (PanelToolController.selected.size() > 0) {
                if (!PanelToolController.selected.contains(btn)) return false;
                for (PanelButtonQuest b : PanelToolController.selected) line.remove(
                    b.getStoredValue()
                        .getKey());
            } else {
                UUID qID = btn.getStoredValue()
                    .getKey();
                line.remove(qID);
            }

            // Sync Line
            NBTTagCompound chPayload = new NBTTagCompound();
            NBTTagList cdList = new NBTTagList();
            NBTTagCompound cTag = new NBTTagCompound();
            NBTConverter.UuidValueType.QUEST_LINE.writeId(QuestLineDatabase.INSTANCE.lookupKey(line), cTag);
            cTag.setTag("config", line.writeToNBT(new NBTTagCompound(), null));
            cdList.appendTag(cTag);
            chPayload.setTag("data", cdList);
            chPayload.setInteger("action", 0);
            NetChapterEdit.sendEdit(chPayload);
            return true;
        }

        return false;
    }

    @Override
    public boolean onMouseRelease(int mx, int my, int click) {
        return false;
    }

    @Override
    public void drawCanvas(int mx, int my, float partialTick) {}

    @Override
    public void drawOverlay(int mx, int my, float partialTick) {}

    @Override
    public List<String> getTooltip(int mx, int my) {
        return null;
    }

    @Override
    public boolean onMouseScroll(int mx, int my, int scroll) {
        return false;
    }

    @Override
    public boolean onKeyPressed(char c, int key) {
        if (PanelToolController.selected.size() > 0 && key == Keyboard.KEY_RETURN) {
            IQuestLine line = gui.getQuestLine();
            for (PanelButtonQuest b : PanelToolController.selected) line.remove(
                b.getStoredValue()
                    .getKey());

            // Sync Line
            NBTTagCompound chPayload = new NBTTagCompound();
            NBTTagList cdList = new NBTTagList();
            NBTTagCompound cTag = new NBTTagCompound();
            NBTConverter.UuidValueType.QUEST_LINE.writeId(QuestLineDatabase.INSTANCE.lookupKey(line), cTag);
            cTag.setTag("config", line.writeToNBT(new NBTTagCompound(), null));
            cdList.appendTag(cTag);
            chPayload.setTag("data", cdList);
            chPayload.setInteger("action", 0);
            NetChapterEdit.sendEdit(chPayload);
            return true;
        }

        return false;
    }

    @Override
    public boolean clampScrolling() {
        return true;
    }

    @Override
    public void onSelection(List<PanelButtonQuest> buttons) {}

    @Override
    public boolean useSelection() {
        return true;
    }
}
