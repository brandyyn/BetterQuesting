package betterquesting.client;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import betterquesting.core.BetterQuesting;
import cpw.mods.fml.client.registry.ClientRegistry;

public class BQ_Keybindings {

    public static KeyBinding openQuests;

    public static void RegisterKeys() {
        openQuests = new KeyBinding("key.betterquesting.quests", Keyboard.KEY_GRAVE, BetterQuesting.NAME);

        ClientRegistry.registerKeyBinding(openQuests);
    }
}
