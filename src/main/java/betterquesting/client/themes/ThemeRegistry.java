package betterquesting.client.themes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import betterquesting.api.storage.BQ_Settings;
import betterquesting.api.utils.JsonHelper;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.resources.colors.IGuiColor;
import betterquesting.api2.client.gui.resources.lines.IGuiLine;
import betterquesting.api2.client.gui.resources.lines.SimpleLine;
import betterquesting.api2.client.gui.resources.textures.IGuiTexture;
import betterquesting.api2.client.gui.resources.textures.SlicedTexture;
import betterquesting.api2.client.gui.themes.GuiKey;
import betterquesting.api2.client.gui.themes.IGuiTheme;
import betterquesting.api2.client.gui.themes.IThemeRegistry;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetGUIs;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.registry.IFactoryData;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.editors.GuiFileBrowser;
import betterquesting.client.gui2.editors.GuiTextEditor;
import betterquesting.client.gui2.editors.nbt.GuiEntitySelection;
import betterquesting.client.gui2.editors.nbt.GuiFluidSelection;
import betterquesting.client.gui2.editors.nbt.GuiItemSelection;
import betterquesting.client.gui2.editors.nbt.GuiNbtEditor;
import betterquesting.core.BetterQuesting;
import betterquesting.handlers.ConfigHandler;

public class ThemeRegistry implements IThemeRegistry {

    public static final ThemeRegistry INSTANCE = new ThemeRegistry();

    private static final IGuiTexture NULL_TEXTURE = new SlicedTexture(
        PresetTexture.TX_NULL,
        new GuiRectangle(0, 0, 32, 32),
        new GuiPadding(8, 8, 8, 8));
    private static final IGuiLine NULL_LINE = new SimpleLine();
    private static final IGuiColor NULL_COLOR = new GuiColorStatic(0xFF000000);

    private final HashMap<ResourceLocation, IGuiTexture> defTextures = new HashMap<>();
    private final HashMap<ResourceLocation, IGuiLine> defLines = new HashMap<>();
    private final HashMap<ResourceLocation, IGuiColor> defColors = new HashMap<>();
    private final HashMap<GuiKey<?>, Function<?, GuiScreen>> defGuis = new HashMap<>();

    private final HashMap<ResourceLocation, IGuiTheme> themes = new HashMap<>();
    private final List<ResourceLocation> loadedThemes = new ArrayList<>();

    private final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private IGuiTheme activeTheme = null;

    private boolean setup = false;

    public ThemeRegistry() {
        PresetTexture.registerTextures(this);
        PresetIcon.registerIcons(this);
        PresetLine.registerLines(this);
        PresetColor.registerColors(this);

        setDefaultGui(PresetGUIs.HOME, arg -> new GuiHome(arg.parent));

        setDefaultGui(PresetGUIs.EDIT_NBT, arg -> {
            if (arg.value instanceof NBTTagCompound) {
                // noinspection unchecked
                return new GuiNbtEditor(arg.parent, (NBTTagCompound) arg.value, arg.callback);
            } else if (arg.value instanceof NBTTagList) {
                // noinspection unchecked
                return new GuiNbtEditor(arg.parent, (NBTTagList) arg.value, arg.callback);
            } else {
                return null;
            }
        });

        setDefaultGui(PresetGUIs.EDIT_ITEM, arg -> new GuiItemSelection(arg.parent, arg.value, arg.callback));
        setDefaultGui(PresetGUIs.EDIT_FLUID, arg -> new GuiFluidSelection(arg.parent, arg.value, arg.callback));
        setDefaultGui(PresetGUIs.EDIT_ENTITY, arg -> new GuiEntitySelection(arg.parent, arg.value, arg.callback));
        setDefaultGui(PresetGUIs.EDIT_TEXT, arg -> new GuiTextEditor(arg.parent, arg.value, arg.callback));
        setDefaultGui(
            PresetGUIs.FILE_EXPLORE,
            arg -> new GuiFileBrowser(arg.parent, arg.callback, arg.root, arg.filter)
                .allowMultiSelect(arg.multiSelect));
    }

    @Override
    public void registerTheme(IGuiTheme theme) {
        if (theme == null || theme.getID() == null) {
            throw new NullPointerException("Cannot register null theme");
        } else if (themes.containsKey(theme.getID())) {
            throw new IllegalArgumentException("Cannot register duplicate theme: " + theme.getID());
        }

        themes.put(theme.getID(), theme);

        if (activeTheme == null) setup = false; // A theme was registered that could possibly resolve the currently
                                                // configured theme
    }

    /**
     * Sets the default fallback texture. Only use if you're defining your own custom texture ID
     */
    @Override
    public void setDefaultTexture(ResourceLocation key, IGuiTexture tex) {
        if (key == null || tex == null) {
            throw new NullPointerException("Tried to register a default theme texture with one or more NULL arguments");
        }

        defTextures.put(key, tex);
    }

    /**
     * Sets the default fallback texture. Only use if you're defining your own custom texture ID
     */
    @Override
    public void setDefaultLine(ResourceLocation key, IGuiLine line) {
        if (key == null || line == null) {
            throw new NullPointerException("Tried to register a default theme line with one or more NULL arguments");
        }

        defLines.put(key, line);
    }

    /**
     * Sets the default fallback texture. Only use if you're defining your own custom texture ID
     */
    @Override
    public void setDefaultColor(ResourceLocation key, IGuiColor color) {
        if (key == null || color == null) {
            throw new NullPointerException("Tried to register default theme colour with one or more NULL arguments");
        }

        defColors.put(key, color);
    }

    @Override
    public <T> void setDefaultGui(GuiKey<T> key, Function<T, GuiScreen> func) {
        if (key == null || func == null) {
            throw new NullPointerException("Tried to register a default gui with one or more NULL arguments");
        }

        defGuis.put(key, func);
    }

    @Override
    public void setTheme(ResourceLocation id) {
        setTheme(themes.get(id), id);
    }

    @Override
    public IGuiTheme getTheme(ResourceLocation key) {
        if (key == null) return null;
        return themes.get(key);
    }

    private void setTheme(IGuiTheme theme, ResourceLocation id) {
        this.activeTheme = theme;

        BQ_Settings.curTheme = id == null ? "" : id.toString();

        if (ConfigHandler.config != null) {
            ConfigHandler.config.get(Configuration.CATEGORY_GENERAL, "Theme", "")
                .set(BQ_Settings.curTheme);
            ConfigHandler.config.save();
        } else {
            BetterQuesting.logger.log(Level.WARN, "Unable to save theme setting");
        }
    }

    @Override
    public IGuiTheme getCurrentTheme() {
        if (!setup && this.activeTheme == null) {
            this.activeTheme = this.getTheme(new ResourceLocation(BQ_Settings.curTheme));
            setup = true;
        }

        return this.activeTheme;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadResourceThemes() {
        loadedThemes.forEach(themes::remove);
        loadedThemes.clear();

        IResourceManager resManager = Minecraft.getMinecraft()
            .getResourceManager();

        Set<String> encounteredJsons = new HashSet<>();

        for (String domain : (Set<String>) resManager.getResourceDomains()) {
            // Skip the other embedded mods
            if ("bq_standard".equals(domain) || "questbook".equals(domain) || "CB4BQ".equals(domain)) {
                continue;
            }
            ResourceLocation res = new ResourceLocation(domain, "bq_themes.json");
            List<IResource> list;

            try {
                list = (List<IResource>) resManager.getAllResources(res);
            } catch (Exception e) {
                continue;
            } // Not going to log errors everytime the file isn't found

            for (IResource iresource : list) {
                try {
                    String jsonStr = IOUtils.toString(iresource.getInputStream(), StandardCharsets.UTF_8);
                    String signature = DigestUtils.sha256Hex(jsonStr);
                    if (!encounteredJsons.add(signature)) {
                        // Already parsed an identical json file.
                        continue;
                    }
                    JsonArray jAry = GSON.fromJson(jsonStr, JsonArray.class);

                    for (int i = 0; i < jAry.size(); i++) {
                        JsonElement je = jAry.get(i);

                        if (!(je instanceof JsonObject)) {
                            BetterQuesting.logger
                                .log(Level.WARN, "Invalid theme entry at index " + i + " in " + domain);
                            continue;
                        }

                        JsonObject jThm = je.getAsJsonObject();

                        if (jThm.has("themeType")) {
                            BetterQuesting.logger.warn("Deprecated legacy theme entry " + i + " in " + domain);
                            BetterQuesting.logger.warn("Please convert this to the new format");
                            loadLegacy(jThm, domain);
                            continue;
                        }

                        ResourceLocation parentID = !jThm.has("themeParent") ? null
                            : new ResourceLocation(JsonHelper.GetString(jThm, "themeParent", "minecraft:null"));
                        String themeName = JsonHelper.GetString(jThm, "themeName", "Unnamed Theme");
                        String idRaw = JsonHelper.GetString(jThm, "themeID", themeName);
                        idRaw = idRaw.toLowerCase()
                            .trim()
                            .replaceAll(" ", "_");
                        if (!idRaw.contains(":")) idRaw = domain + ":" + idRaw;
                        ResourceLocation themeId = new ResourceLocation(idRaw);

                        int n = 0;
                        while (themes.containsKey(themeId)) themeId = new ResourceLocation(domain, idRaw + n++);

                        ResourceTheme resTheme;

                        try {
                            resTheme = new ResourceTheme(parentID, themeId, themeName);
                        } catch (Exception e) {
                            BetterQuesting.logger.error("Failed to load theme entry " + i + " in " + domain, e);
                            continue;
                        }

                        JsonObject jsonTextureRoot = JsonHelper.GetObject(jThm, "textures");
                        for (Entry<String, JsonElement> entry : jsonTextureRoot.entrySet()) {
                            if (!entry.getValue()
                                .isJsonObject()) continue;
                            JsonObject joTex = entry.getValue()
                                .getAsJsonObject();

                            ResourceLocation typeID = new ResourceLocation(
                                JsonHelper.GetString(joTex, "textureType", ""));
                            IFactoryData<IGuiTexture, JsonObject> tFact = ResourceRegistry.INSTANCE.getTexReg()
                                .getFactory(typeID);

                            if (tFact == null) {
                                BetterQuesting.logger.error(
                                    "Unknown texture type " + typeID + " for theme " + themeName + " in " + domain);
                                continue;
                            }

                            IGuiTexture gTex = tFact.loadFromData(joTex);

                            if (gTex == null) {
                                BetterQuesting.logger.error(
                                    "Failed to load texture type " + typeID
                                        + " for theme "
                                        + themeName
                                        + " in "
                                        + domain);
                                continue;
                            }

                            resTheme.setTexture(new ResourceLocation(entry.getKey()), gTex);
                        }

                        JsonObject jsonColourRoot = JsonHelper.GetObject(jThm, "colors");
                        for (Entry<String, JsonElement> entry : jsonColourRoot.entrySet()) {
                            if (!(entry.getValue() instanceof JsonObject)) continue;
                            JsonObject joCol = entry.getValue()
                                .getAsJsonObject();

                            ResourceLocation typeID = new ResourceLocation(
                                JsonHelper.GetString(joCol, "colorType", ""));
                            IFactoryData<IGuiColor, JsonObject> cFact = ResourceRegistry.INSTANCE.getColorReg()
                                .getFactory(typeID);

                            if (cFact == null) {
                                BetterQuesting.logger.error(
                                    "Unknown color type " + typeID + " for theme " + themeName + " in " + domain);
                                continue;
                            }

                            IGuiColor gCol = cFact.loadFromData(joCol);

                            if (gCol == null) {
                                BetterQuesting.logger.error(
                                    "Failed to load color type " + typeID
                                        + " for theme "
                                        + themeName
                                        + " in "
                                        + domain);
                                continue;
                            }

                            resTheme.setColor(new ResourceLocation(entry.getKey()), gCol);
                        }

                        JsonObject jsonLinesRoot = JsonHelper.GetObject(jThm, "lines");
                        for (Entry<String, JsonElement> entry : jsonLinesRoot.entrySet()) {
                            if (!(entry.getValue() instanceof JsonObject)) continue;
                            JsonObject joLine = entry.getValue()
                                .getAsJsonObject();

                            ResourceLocation typeID = new ResourceLocation(
                                JsonHelper.GetString(joLine, "lineType", ""));
                            IFactoryData<IGuiLine, JsonObject> lFact = ResourceRegistry.INSTANCE.getLineReg()
                                .getFactory(typeID);

                            if (lFact == null) {
                                BetterQuesting.logger
                                    .error("Unknown line type " + typeID + " for theme " + themeName + " in " + domain);
                                continue;
                            }

                            IGuiLine gLine = lFact.loadFromData(joLine);

                            if (gLine == null) {
                                BetterQuesting.logger.error(
                                    "Failed to load line type " + typeID + " for theme " + themeName + " in " + domain);
                                continue;
                            }

                            resTheme.setLine(new ResourceLocation(entry.getKey()), gLine);
                        }

                        themes.put(resTheme.getID(), resTheme);
                        loadedThemes.add(resTheme.getID());
                    }
                } catch (Exception e) {
                    BetterQuesting.logger.error("Error reading bq_themes.json from " + domain, e);
                }
            }
        }
    }

    @Deprecated
    private void loadLegacy(JsonObject json, String domain) {
        IGuiTheme theme = LegacyThemeLoader.INSTANCE.loadTheme(json, domain);

        if (theme == null) {
            BetterQuesting.logger.error("Failed to load legacy theme from " + domain);
            return;
        } else if (themes.containsKey(theme.getID())) {
            BetterQuesting.logger.error("Unable to register legacy resource theme with duplicate ID: " + theme.getID());
        }

        themes.put(theme.getID(), theme);
        loadedThemes.add(theme.getID());
    }

    @Override
    public IGuiTexture getTexture(ResourceLocation key) {
        if (key == null) return NULL_TEXTURE;

        IGuiTexture tex = null;

        if (getCurrentTheme() != null) tex = activeTheme.getTexture(key);
        if (tex == null) tex = defTextures.get(key);
        return tex == null ? NULL_TEXTURE : tex;
    }

    @Override
    public IGuiLine getLine(ResourceLocation key) {
        if (key == null) return NULL_LINE;

        IGuiLine line = null;

        if (getCurrentTheme() != null) line = activeTheme.getLine(key);
        if (line == null) line = defLines.get(key);
        return line == null ? NULL_LINE : line;
    }

    @Override
    public IGuiColor getColor(ResourceLocation key) {
        if (key == null) return NULL_COLOR;

        IGuiColor color = null;

        if (getCurrentTheme() != null) color = activeTheme.getColor(key);
        if (color == null) color = defColors.get(key);
        return color == null ? NULL_COLOR : color;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> GuiScreen getGui(GuiKey<T> key, T args) {
        if (key == null) return null;

        Function<T, GuiScreen> func = null;

        if (getCurrentTheme() != null) func = activeTheme.getGui(key);
        if (func == null) func = (Function<T, GuiScreen>) defGuis.get(key);

        return func == null ? null : func.apply(args);
    }

    @Override
    public List<IGuiTheme> getAllThemes() {
        ArrayList<IGuiTheme> sorted = new ArrayList<>(themes.values());
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }

    @Override
    public ResourceLocation[] getKnownTextures() {
        return defTextures.keySet()
            .toArray(new ResourceLocation[0]);
    }

    @Override
    public ResourceLocation[] getKnownColors() {
        return defColors.keySet()
            .toArray(new ResourceLocation[0]);
    }

    @Override
    public ResourceLocation[] getKnownLines() {
        return defLines.keySet()
            .toArray(new ResourceLocation[0]);
    }

    @Override
    public GuiKey[] getKnownGuis() {
        return defGuis.keySet()
            .toArray(new GuiKey[0]);
    }
}
