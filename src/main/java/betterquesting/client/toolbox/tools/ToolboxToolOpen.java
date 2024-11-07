package betterquesting.client.toolbox.tools;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import betterquesting.api.client.toolbox.IToolboxTool;
import betterquesting.api2.client.gui.controls.PanelButtonQuest;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestLine;
import betterquesting.client.gui2.GuiQuest;

public class ToolboxToolOpen implements IToolboxTool {

    private CanvasQuestLine gui;

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

        PanelButtonQuest btn = gui.getButtonAt(mx, my);

        if (btn != null) {
            UUID qID = btn.getStoredValue()
                .getKey();

            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new GuiQuest(mc.currentScreen, qID));
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
        return false;
    }
}
