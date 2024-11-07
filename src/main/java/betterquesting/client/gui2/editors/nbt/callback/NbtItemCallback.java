package betterquesting.client.gui2.editors.nbt.callback;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import betterquesting.api.misc.ICallback;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api.utils.JsonHelper;

public class NbtItemCallback implements ICallback<BigItemStack> {

    private final NBTTagCompound json;

    public NbtItemCallback(NBTTagCompound json) {
        this.json = json;
    }

    public void setValue(BigItemStack stack) {
        BigItemStack baseStack;

        if (stack != null) {
            baseStack = stack;
        } else {
            baseStack = new BigItemStack(Blocks.stone);
        }

        JsonHelper.ClearCompoundTag(json);
        JsonHelper.ItemStackToJson(baseStack, json);
    }
}
