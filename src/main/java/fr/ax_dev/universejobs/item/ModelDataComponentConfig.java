package fr.ax_dev.universejobs.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

// encapsulates the configuration for an item's custom model data component

public final class ModelDataComponentConfig {

    private static final ModelDataComponentConfig EMPTY = new ModelDataComponentConfig(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null
    );

    private final List<Float> floats;
    private final List<Boolean> flags;
    private final List<String> strings;
    private final List<Color> colors;
    private final Integer legacyCustomModelData;
    private final String legacyStringValue;

    private ModelDataComponentConfig(
            List<Float> floats,
            List<Boolean> flags,
            List<String> strings,
            List<Color> colors,
            Integer legacyCustomModelData,
            String legacyStringValue
    ) {
        this.floats = Collections.unmodifiableList(new ArrayList<>(floats));
        this.flags = Collections.unmodifiableList(new ArrayList<>(flags));
        this.strings = Collections.unmodifiableList(new ArrayList<>(strings));
        this.colors = Collections.unmodifiableList(new ArrayList<>(colors));
        this.legacyCustomModelData = legacyCustomModelData;
        this.legacyStringValue = legacyStringValue;
    }

    public static ModelDataComponentConfig fromSection(ConfigurationSection section) {
        if (section == null) {
            return empty();
        }

        ConfigurationSection componentSection = section.getConfigurationSection("model_data_component");
        List<Float> floats = new ArrayList<>();
        List<Boolean> flags = new ArrayList<>();
        List<String> strings = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        Integer legacyValue = null;
        String legacyString = null;

        if (componentSection != null) {
            floats.addAll(extractFloatList(componentSection, "floats"));
            flags.addAll(extractBooleanList(componentSection, "flags"));
            strings.addAll(componentSection.getStringList("strings"));
            colors.addAll(extractColorList(componentSection, "colors"));
        }

        if (section.contains("custom-model-data")) {
            Object rawValue = section.get("custom-model-data");

            if (rawValue instanceof Number number) {
                legacyValue = number.intValue();
            } else if (rawValue instanceof String strValue) {
                String trimmed = strValue.trim();
                if (!trimmed.isEmpty()) {
                    if (looksNumeric(trimmed)) {
                        try {
                            legacyValue = Integer.parseInt(trimmed);
                        } catch (NumberFormatException ignored) {
                            legacyString = trimmed;
                        }
                    } else {
                        legacyString = trimmed;
                    }
                }
            }
        }

        if (legacyString != null && strings.isEmpty()) {
            strings.add(legacyString);
        }

        if (floats.isEmpty() && flags.isEmpty() && strings.isEmpty() && colors.isEmpty() && legacyValue == null) {
            return empty();
        }

        return new ModelDataComponentConfig(floats, flags, strings, colors, legacyValue, legacyString);
    }

    public static ModelDataComponentConfig fromLegacy(int customModelData) {
        return new ModelDataComponentConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                customModelData,
                null
        );
    }

    public static ModelDataComponentConfig fromStrings(List<String> values) {
        return values == null || values.isEmpty()
                ? empty()
                : new ModelDataComponentConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                values,
                Collections.emptyList(),
                null,
                null
        );
    }

    public static ModelDataComponentConfig empty() {
        return EMPTY;
    }

    public boolean hasComponentData() {
        return !floats.isEmpty() || !flags.isEmpty() || !strings.isEmpty() || !colors.isEmpty();
    }

    public boolean hasLegacyCustomModelData() {
        return legacyCustomModelData != null;
    }

    public Optional<Integer> getLegacyCustomModelData() {
        return Optional.ofNullable(legacyCustomModelData);
    }

    public Optional<String> getLegacyStringValue() {
        return Optional.ofNullable(legacyStringValue);
    }

    public List<String> getStrings() {
        return strings;
    }

    public List<Float> getFloats() {
        return floats;
    }

    public List<Boolean> getFlags() {
        return flags;
    }

    public List<Color> getColors() {
        return colors;
    }

    public Optional<String> getFirstString() {
        return strings.isEmpty() ? Optional.empty() : Optional.ofNullable(strings.get(0));
    }

    public boolean isEmpty() {
        return !hasComponentData() && !hasLegacyCustomModelData() && legacyStringValue == null;
    }


    public CustomModelDataComponent toComponent() {
        Map<String, Object> values = createComponentValueMap(true);
        if (values.isEmpty()) {
            return null;
        }
        Object deserialized = ConfigurationSerialization.deserializeObject(values);
        if (deserialized instanceof CustomModelDataComponent component) {
            return component;
        }

        // fallback to legacy alias if the craft implementation name is not recognised yet..
        values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "CustomModelDataComponent");
        deserialized = ConfigurationSerialization.deserializeObject(values);
        if (deserialized instanceof CustomModelDataComponent component) {
            return component;
        }
        return null;
    }

    public void applyTo(org.bukkit.inventory.meta.ItemMeta meta) {
        if (meta == null) {
            return;
        }

        CustomModelDataComponent component = toComponent();
        if (component != null) {
            meta.setCustomModelDataComponent(component);
            return;
        }

        if (legacyCustomModelData != null) {
            meta.setCustomModelData(legacyCustomModelData);
            return;
        }

        // clear any previously set data when the configuration is empty
        meta.setCustomModelDataComponent(null);
        meta.setCustomModelData(null);
    }

    public Map<String, Object> toConfigurationValues() {
        Map<String, Object> values = new LinkedHashMap<>();

        Map<String, Object> componentValues = createComponentValueMap(false);
        if (!componentValues.isEmpty()) {
            values.put("model_data_component", componentValues);
        }

        if (legacyCustomModelData != null) {
            values.put("custom-model-data", legacyCustomModelData);
        } else if (legacyStringValue != null && !legacyStringValue.isEmpty() && !values.containsKey("custom-model-data")) {
            values.put("custom-model-data", legacyStringValue);
        }

        return values;
    }

    private static List<Float> extractFloatList(ConfigurationSection section, String key) {
        List<Float> values = new ArrayList<>();
        if (!section.isList(key)) {
            return values;
        }
        for (Object entry : section.getList(key)) {
            if (entry instanceof Number number) {
                values.add(number.floatValue());
            } else if (entry instanceof String str && looksNumeric(str)) {
                try {
                    values.add(Float.parseFloat(str));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return values;
    }

    private static List<Boolean> extractBooleanList(ConfigurationSection section, String key) {
        List<Boolean> values = new ArrayList<>();
        if (!section.isList(key)) {
            return values;
        }
        for (Object entry : section.getList(key)) {
            if (entry instanceof Boolean bool) {
                values.add(bool);
            } else if (entry instanceof String str) {
                String normalized = str.trim().toLowerCase(Locale.ROOT);
                if ("true".equals(normalized) || "false".equals(normalized)) {
                    values.add(Boolean.parseBoolean(normalized));
                }
            }
        }
        return values;
    }

    private static List<Color> extractColorList(ConfigurationSection section, String key) {
        List<Color> values = new ArrayList<>();
        if (!section.isList(key)) {
            return values;
        }
        for (Object entry : section.getList(key)) {
            if (entry instanceof Color color) {
                values.add(color);
            } else if (entry instanceof String str) {
                parseColor(str).ifPresent(values::add);
            }
        }
        return values;
    }

    private static Optional<Color> parseColor(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        if (trimmed.startsWith("#") && (trimmed.length() == 7 || trimmed.length() == 9)) {
            String hex = trimmed.substring(1);
            try {
                int rgb = (int) Long.parseLong(hex, 16);
                if (hex.length() == 6) {
                    return Optional.of(Color.fromRGB(rgb));
                }
                return Optional.of(Color.fromRGB(rgb & 0xFFFFFF));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        String[] split = trimmed.split("[,:]");
        if (split.length == 3) {
            try {
                int r = clampColor(Integer.parseInt(split[0].trim()));
                int g = clampColor(Integer.parseInt(split[1].trim()));
                int b = clampColor(Integer.parseInt(split[2].trim()));
                return Optional.of(Color.fromRGB(r, g, b));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static boolean looksNumeric(String value) {
        String normalized = value.replaceAll("[_\\s]", "");
        if (normalized.isEmpty()) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (!Character.isDigit(c) && !(c == '-' && i == 0)) {
                return false;
            }
        }
        return true;
    }


    private Map<String, Object> createComponentValueMap(boolean includeTypeKey) {
        if (!hasComponentData()) {
            return Collections.emptyMap();
        }

        Map<String, Object> component = new LinkedHashMap<>();
        if (includeTypeKey) {
            component.put(
                    ConfigurationSerialization.SERIALIZED_TYPE_KEY,
                    "org.bukkit.craftbukkit.inventory.components.CraftCustomModelDataComponent"
            );
        }
        if (!floats.isEmpty()) {
            component.put("floats", new ArrayList<>(floats));
        }
        if (!flags.isEmpty()) {
            component.put("flags", new ArrayList<>(flags));
        }
        if (!strings.isEmpty()) {
            component.put("strings", new ArrayList<>(strings));
        }
        if (!colors.isEmpty()) {
            component.put("colors", new ArrayList<>(colors));
        }
        return component;
    }
}
