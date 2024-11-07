package betterquesting.api2.client.gui.resources.textures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonObject;

import betterquesting.api.utils.JsonHelper;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.resources.colors.IGuiColor;
import cpw.mods.fml.client.config.GuiUtils;

public class SlicedTexture implements IGuiTexture {

    private static final IGuiColor defColor = new GuiColorStatic(255, 255, 255, 255);

    private final ResourceLocation texture;
    private final IGuiRect texBounds;
    private final GuiPadding texBorder;
    private SliceMode sliceMode = SliceMode.SLICED_TILE;

    public SlicedTexture(ResourceLocation tex, IGuiRect bounds, GuiPadding border) {
        this.texture = tex;
        this.texBounds = bounds;
        this.texBorder = border;
    }

    @Override
    public void drawTexture(int x, int y, int width, int height, float zLevel, float partialTick) {
        drawTexture(x, y, width, height, zLevel, partialTick, defColor);
    }

    @Override
    public void drawTexture(int x, int y, int width, int height, float zLevel, float partialTick, IGuiColor color) {
        if (width <= 0 || height <= 0) return;

        int w = Math.max(width, texBorder.getLeft() + texBorder.getRight());
        int h = Math.max(height, texBorder.getTop() + texBorder.getBottom());
        int dx = x;
        int dy = y;

        GL11.glPushMatrix();
        color.applyGlColor();

        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        if (w != width || h != height) {
            dx = 0;
            dy = 0;
            GL11.glTranslatef(x, y, 0);
            GL11.glScaled(width / (double) w, height / (double) h, 1D);
        }

        if (sliceMode == SliceMode.SLICED_TILE) {
            drawContinuousTexturedBox(
                texture,
                dx,
                dy,
                texBounds.getX(),
                texBounds.getY(),
                w,
                h,
                texBounds.getWidth(),
                texBounds.getHeight(),
                texBorder.getTop(),
                texBorder.getBottom(),
                texBorder.getLeft(),
                texBorder.getRight(),
                zLevel);
        } else if (sliceMode == SliceMode.SLICED_STRETCH) {
            int iu = texBounds.getX() + texBorder.getLeft();
            int iv = texBounds.getY() + texBorder.getTop();
            int iw = texBounds.getWidth() - texBorder.getLeft() - texBorder.getRight();
            int ih = texBounds.getHeight() - texBorder.getTop() - texBorder.getBottom();

            float sx = (float) (w - (texBounds.getWidth() - iw)) / (float) iw;
            float sy = (float) (h - (texBounds.getHeight() - ih)) / (float) ih;

            Minecraft.getMinecraft().renderEngine.bindTexture(texture);

            // TOP LEFT
            GL11.glPushMatrix();
            GL11.glTranslatef(dx, dy, 0F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX(),
                texBounds.getY(),
                texBorder.getLeft(),
                texBorder.getTop(),
                zLevel);
            GL11.glPopMatrix();

            // TOP SIDE
            GL11.glPushMatrix();
            GL11.glTranslatef(dx + texBorder.getLeft(), dy, 0F);
            GL11.glScalef(sx, 1F, 1F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX() + texBorder.getLeft(),
                texBounds.getY(),
                iw,
                texBorder.getTop(),
                zLevel);
            GL11.glPopMatrix();

            // TOP RIGHT
            GL11.glPushMatrix();
            GL11.glTranslatef(dx + w - texBorder.getRight(), dy, 0F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX() + texBorder.getLeft() + iw,
                texBounds.getY(),
                texBorder.getRight(),
                texBorder.getTop(),
                zLevel);
            GL11.glPopMatrix();

            // LEFT SIDE
            GL11.glPushMatrix();
            GL11.glTranslatef(dx, dy + texBorder.getTop(), 0F);
            GL11.glScalef(1F, sy, 1F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX(),
                texBounds.getY() + texBorder.getTop(),
                texBorder.getLeft(),
                ih,
                zLevel);
            GL11.glPopMatrix();

            // MIDDLE
            GL11.glPushMatrix();
            GL11.glTranslatef(dx + texBorder.getLeft(), dy + texBorder.getTop(), 0F);
            GL11.glScalef(sx, sy, 1F);
            GuiUtils.drawTexturedModalRect(0, 0, iu, iv, iw, ih, zLevel);
            GL11.glPopMatrix();

            // RIGHT SIDE
            GL11.glPushMatrix();
            GL11.glTranslatef(dx + w - texBorder.getRight(), dy + texBorder.getTop(), 0F);
            GL11.glScalef(1F, sy, 1F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX() + texBorder.getLeft() + iw,
                texBounds.getY() + texBorder.getTop(),
                texBorder.getRight(),
                ih,
                zLevel);
            GL11.glPopMatrix();

            // BOTTOM LEFT
            GL11.glPushMatrix();
            GL11.glTranslatef(dx, dy + h - texBorder.getBottom(), 0F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX(),
                texBounds.getY() + texBorder.getTop() + ih,
                texBorder.getLeft(),
                texBorder.getBottom(),
                zLevel);
            GL11.glPopMatrix();

            // BOTTOM SIDE
            GL11.glPushMatrix();
            GL11.glTranslatef(dx + texBorder.getLeft(), dy + h - texBorder.getBottom(), 0F);
            GL11.glScalef(sx, 1F, 1F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX() + texBorder.getLeft(),
                texBounds.getY() + texBorder.getTop() + ih,
                iw,
                texBorder.getBottom(),
                zLevel);
            GL11.glPopMatrix();

            // BOTTOM RIGHT
            GL11.glPushMatrix();
            GL11.glTranslatef(dx + w - texBorder.getRight(), dy + h - texBorder.getBottom(), 0F);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX() + texBorder.getLeft() + iw,
                texBounds.getY() + texBorder.getTop() + ih,
                texBorder.getRight(),
                texBorder.getBottom(),
                zLevel);
            GL11.glPopMatrix();
        } else {
            float sx = (float) w / (float) texBounds.getWidth();
            float sy = (float) h / (float) texBounds.getHeight();
            GL11.glTranslatef(dx, dy, 0F);
            GL11.glScalef(sx, sy, 1F);

            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

            Minecraft.getMinecraft().renderEngine.bindTexture(texture);
            GuiUtils.drawTexturedModalRect(
                0,
                0,
                texBounds.getX(),
                texBounds.getY(),
                texBounds.getWidth(),
                texBounds.getHeight(),
                zLevel);
        }

        GL11.glPopMatrix();
    }

    @Override
    public ResourceLocation getTexture() {
        return this.texture;
    }

    @Override
    public IGuiRect getBounds() {
        return this.texBounds;
    }

    public GuiPadding getBorder() {
        return this.texBorder;
    }

    /**
     * Enables texture slicing. Will stretch to fit if disabled
     */
    public SlicedTexture setSliceMode(SliceMode mode) {
        this.sliceMode = mode;
        return this;
    }

    public static SlicedTexture readFromJson(JsonObject json) {
        ResourceLocation res = new ResourceLocation(JsonHelper.GetString(json, "texture", "minecraft:missingno"));
        int slice = JsonHelper.GetNumber(json, "sliceMode", 1)
            .intValue();

        JsonObject jOut = JsonHelper.GetObject(json, "coordinates");
        int ox = JsonHelper.GetNumber(jOut, "u", 0)
            .intValue();
        int oy = JsonHelper.GetNumber(jOut, "v", 0)
            .intValue();
        int ow = JsonHelper.GetNumber(jOut, "w", 48)
            .intValue();
        int oh = JsonHelper.GetNumber(jOut, "h", 48)
            .intValue();

        JsonObject jIn = JsonHelper.GetObject(json, "border");
        int il = JsonHelper.GetNumber(jIn, "l", 16)
            .intValue();
        int it = JsonHelper.GetNumber(jIn, "t", 16)
            .intValue();
        int ir = JsonHelper.GetNumber(jIn, "r", 16)
            .intValue();
        int ib = JsonHelper.GetNumber(jIn, "b", 16)
            .intValue();

        return new SlicedTexture(res, new GuiRectangle(ox, oy, ow, oh), new GuiPadding(il, it, ir, ib))
            .setSliceMode(SliceMode.values()[slice % 3]);
    }

    // Slightly modified version from GuiUtils.class
    private static void drawContinuousTexturedBox(ResourceLocation res, int x, int y, int u, int v, int width,
        int height, int textureWidth, int textureHeight, int topBorder, int bottomBorder, int leftBorder,
        int rightBorder, float zLevel) {
        Minecraft.getMinecraft().renderEngine.bindTexture(res);

        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int fillerWidth = textureWidth - leftBorder - rightBorder;
        int fillerHeight = textureHeight - topBorder - bottomBorder;
        if (fillerWidth <= 0 || fillerHeight <= 0) return;
        int canvasWidth = width - leftBorder - rightBorder;
        int canvasHeight = height - topBorder - bottomBorder;
        int xPasses = canvasWidth / fillerWidth;
        int remainderWidth = canvasWidth % fillerWidth;
        int yPasses = canvasHeight / fillerHeight;
        int remainderHeight = canvasHeight % fillerHeight;

        // Draw Border
        // Top Left
        GuiUtils.drawTexturedModalRect(x, y, u, v, leftBorder, topBorder, zLevel);
        // Top Right
        GuiUtils.drawTexturedModalRect(
            x + leftBorder + canvasWidth,
            y,
            u + leftBorder + fillerWidth,
            v,
            rightBorder,
            topBorder,
            zLevel);
        // Bottom Left
        GuiUtils.drawTexturedModalRect(
            x,
            y + topBorder + canvasHeight,
            u,
            v + topBorder + fillerHeight,
            leftBorder,
            bottomBorder,
            zLevel);
        // Bottom Right
        GuiUtils.drawTexturedModalRect(
            x + leftBorder + canvasWidth,
            y + topBorder + canvasHeight,
            u + leftBorder + fillerWidth,
            v + topBorder + fillerHeight,
            rightBorder,
            bottomBorder,
            zLevel);

        for (int i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); i++) {
            // Top Border
            GuiUtils.drawTexturedModalRect(
                x + leftBorder + (i * fillerWidth),
                y,
                u + leftBorder,
                v,
                (i == xPasses ? remainderWidth : fillerWidth),
                topBorder,
                zLevel);
            // Bottom Border
            GuiUtils.drawTexturedModalRect(
                x + leftBorder + (i * fillerWidth),
                y + topBorder + canvasHeight,
                u + leftBorder,
                v + topBorder + fillerHeight,
                (i == xPasses ? remainderWidth : fillerWidth),
                bottomBorder,
                zLevel);

            // Throw in some filler for good measure
            for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) GuiUtils.drawTexturedModalRect(
                x + leftBorder + (i * fillerWidth),
                y + topBorder + (j * fillerHeight),
                u + leftBorder,
                v + topBorder,
                (i == xPasses ? remainderWidth : fillerWidth),
                (j == yPasses ? remainderHeight : fillerHeight),
                zLevel);
        }

        // Side Borders
        for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
            // Left Border
            GuiUtils.drawTexturedModalRect(
                x,
                y + topBorder + (j * fillerHeight),
                u,
                v + topBorder,
                leftBorder,
                (j == yPasses ? remainderHeight : fillerHeight),
                zLevel);
            // Right Border
            GuiUtils.drawTexturedModalRect(
                x + leftBorder + canvasWidth,
                y + topBorder + (j * fillerHeight),
                u + leftBorder + fillerWidth,
                v + topBorder,
                rightBorder,
                (j == yPasses ? remainderHeight : fillerHeight),
                zLevel);
        }
    }

    public enum SliceMode {
        STRETCH,
        SLICED_TILE,
        SLICED_STRETCH
    }
}
