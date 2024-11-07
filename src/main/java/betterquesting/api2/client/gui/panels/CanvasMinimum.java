package betterquesting.api2.client.gui.panels;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;

import betterquesting.api2.client.gui.misc.ComparatorGuiDepth;
import betterquesting.api2.client.gui.misc.IGuiRect;

public class CanvasMinimum implements IGuiCanvas {

    private final List<IGuiPanel> guiPanels = new CopyOnWriteArrayList<>();
    private IGuiRect internalTransform;
    private boolean enabled = true;
    private int finalWidth = 0;
    private int finalHeight = 0;

    public CanvasMinimum(IGuiRect rect) {
        this.internalTransform = rect;
    }

    @Override
    public IGuiRect getTransform() {
        return new IGuiRect() {

            @Override
            public int getX() {
                return internalTransform.getX();
            }

            @Override
            public int getY() {
                return internalTransform.getY();
            }

            @Override
            public int getWidth() {
                return finalWidth;
            }

            @Override
            public int getHeight() {
                return finalHeight;
            }

            @Override
            public int getDepth() {
                return internalTransform.getDepth();
            }

            @Override
            public IGuiRect getParent() {
                return internalTransform.getParent();
            }

            @Override
            public void setParent(IGuiRect rect) {
                internalTransform.setParent(rect);
            }

            @Override
            public boolean contains(int x, int y) {
                return internalTransform.contains(x, y);
            }

            @Override
            public int compareTo(IGuiRect o) {
                return internalTransform.compareTo(o);
            }
        };
    }

    @Override
    public void initPanel() {
        this.guiPanels.clear();
    }

    @Override
    public void setEnabled(boolean state) {
        this.enabled = state;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void drawPanel(int mx, int my, float partialTick) {
        for (IGuiPanel entry : guiPanels) {
            if (entry.isEnabled()) {
                entry.drawPanel(mx, my, partialTick);
            }
        }
    }

    @Override
    public boolean onMouseClick(int mx, int my, int click) {
        boolean used = false;

        ListIterator<IGuiPanel> pnIter = guiPanels.listIterator(guiPanels.size());

        while (pnIter.hasPrevious()) {
            IGuiPanel entry = pnIter.previous();

            if (entry.isEnabled() && entry.onMouseClick(mx, my, click)) {
                used = true;
                break;
            }
        }

        return used;
    }

    @Override
    public boolean onMouseRelease(int mx, int my, int click) {
        boolean used = false;

        ListIterator<IGuiPanel> pnIter = guiPanels.listIterator(guiPanels.size());

        while (pnIter.hasPrevious()) {
            IGuiPanel entry = pnIter.previous();

            if (entry.isEnabled() && entry.onMouseRelease(mx, my, click)) {
                used = true;
                break;
            }
        }

        return used;
    }

    @Override
    public boolean onMouseScroll(int mx, int my, int scroll) {
        boolean used = false;

        ListIterator<IGuiPanel> pnIter = guiPanels.listIterator(guiPanels.size());

        while (pnIter.hasPrevious()) {
            IGuiPanel entry = pnIter.previous();

            if (entry.isEnabled() && entry.onMouseScroll(mx, my, scroll)) {
                used = true;
                break;
            }
        }

        return used;
    }

    @Override
    public boolean onKeyTyped(char c, int keycode) {
        boolean used = false;

        ListIterator<IGuiPanel> pnIter = guiPanels.listIterator(guiPanels.size());

        while (pnIter.hasPrevious()) {
            IGuiPanel entry = pnIter.previous();

            if (entry.isEnabled() && entry.onKeyTyped(c, keycode)) {
                used = true;
                break;
            }
        }

        return used;
    }

    @Override
    public List<String> getTooltip(int mx, int my) {
        ListIterator<IGuiPanel> pnIter = guiPanels.listIterator(guiPanels.size());
        List<String> tt;

        while (pnIter.hasPrevious()) {
            IGuiPanel entry = pnIter.previous();

            if (!entry.isEnabled()) {
                continue;
            }

            tt = entry.getTooltip(mx, my);

            if (tt != null) {
                return tt;
            }
        }

        return null;
    }

    @Override
    public void addPanel(IGuiPanel panel) {
        if (panel == null || guiPanels.contains(panel)) {
            return;
        }

        guiPanels.add(panel);
        guiPanels.sort(ComparatorGuiDepth.INSTANCE);
        panel.getTransform()
            .setParent(getTransform());
        panel.initPanel();
        recalcSizes();
    }

    @Override
    public boolean removePanel(IGuiPanel panel) {
        boolean result = guiPanels.remove(panel);
        recalcSizes();
        return result;
    }

    @Nonnull
    @Override
    public List<IGuiPanel> getChildren() {
        return guiPanels;
    }

    @Override
    public void resetCanvas() {
        guiPanels.clear();
    }

    protected void recalcSizes() {
        int height = 0;
        int width = 0;
        for (IGuiPanel guiPanel : guiPanels) {
            IGuiRect transform = guiPanel.getTransform();
            height = Math.max(height, transform.getY() + transform.getHeight());
            width = Math.max(width, transform.getX() + transform.getWidth());
        }
        finalWidth = width;
        finalHeight = height;
    }
}
