package betterquesting.api2.client.gui.themes;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import betterquesting.api2.client.gui.resources.colors.IGuiColor;
import betterquesting.api2.client.gui.resources.lines.IGuiLine;
import betterquesting.api2.client.gui.resources.textures.IGuiTexture;

public interface IGuiTheme extends Comparable<IGuiTheme> {

    String getName();

    ResourceLocation getID();

    @Nullable
    IGuiTexture getTexture(ResourceLocation key);

    @Nullable
    IGuiLine getLine(ResourceLocation key);

    @Nullable
    IGuiColor getColor(ResourceLocation key);

    @Nullable
    <T> Function<T, GuiScreen> getGui(GuiKey<T> key);

    @Override
    default int compareTo(IGuiTheme iGuiTheme) {
        return this.getName()
            .compareTo(iGuiTheme.getName());
    }
}
