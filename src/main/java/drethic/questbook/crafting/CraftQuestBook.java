package drethic.questbook.crafting;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import cpw.mods.fml.common.registry.GameRegistry;
import drethic.questbook.item.QBItems;

public class CraftQuestBook {

    public static final void init() {
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(QBItems.ItemQuestBook), Items.book, "stickWood"));
    }
}
