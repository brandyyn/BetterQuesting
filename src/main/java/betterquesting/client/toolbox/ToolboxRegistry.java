package betterquesting.client.toolbox;

import java.util.Collection;
import java.util.HashMap;

import net.minecraft.util.ResourceLocation;

import betterquesting.api.client.toolbox.IToolRegistry;
import betterquesting.api2.client.toolbox.IToolTab;

public class ToolboxRegistry implements IToolRegistry {

    public static final ToolboxRegistry INSTANCE = new ToolboxRegistry();

    private final HashMap<ResourceLocation, IToolTab> toolTabs = new HashMap<>();

    @Override
    public void registerToolTab(ResourceLocation tabID, IToolTab tab) {
        if (tabID == null || tab == null) {
            throw new NullPointerException("Tried to register null tab or null ID");
        } else if (toolTabs.containsKey(tabID)) {
            throw new IllegalArgumentException("Cannot register duplicate tab ID: " + tabID);
        }

        toolTabs.put(tabID, tab);
    }

    @Override
    public IToolTab getTabByID(ResourceLocation tabID) {
        return toolTabs.get(tabID);
    }

    @Override
    public Collection<IToolTab> getAllTabs() {
        return toolTabs.values();
    }
}
