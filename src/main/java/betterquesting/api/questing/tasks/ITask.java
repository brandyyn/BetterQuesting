package betterquesting.api.questing.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.INBTProgress;
import betterquesting.api2.storage.INBTSaveLoad;
import betterquesting.api2.utils.ParticipantInfo;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface ITask extends INBTSaveLoad<NBTTagCompound>, INBTProgress<NBTTagCompound> {

    String getUnlocalisedName();

    ResourceLocation getFactoryID();

    void detect(ParticipantInfo participant, Map.Entry<UUID, IQuest> quest);

    boolean isComplete(UUID uuid);

    void setComplete(UUID uuid);

    void resetUser(@Nullable UUID uuid);

    @Nullable
    @SideOnly(Side.CLIENT)
    IGuiPanel getTaskGui(IGuiRect rect, Map.Entry<UUID, IQuest> quest);

    @Nullable
    @SideOnly(Side.CLIENT)
    GuiScreen getTaskEditor(GuiScreen parent, Map.Entry<UUID, IQuest> quest);

    /**
     * Tasks that set this to true will be ignored by quest completion logic.
     */
    default boolean ignored(UUID uuid) {
        return false;
    }

    default List<String> getTextsForSearch() {
        return null;
    }
}
