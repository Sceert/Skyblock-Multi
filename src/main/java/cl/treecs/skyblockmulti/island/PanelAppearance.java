package cl.treecs.skyblockmulti.island;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Local presentation preferences. No server or world state is stored here. */
public record PanelAppearance(int width, int height, int background, int header, int border,
                              int label, int value, int accent, int hudX, int hudY) {
    public static final PanelAppearance DEFAULT = new PanelAppearance(260, 240, 0x1C282B, 0x2D4041,
            0x617D73, 0xAFBEB7, 0xEDF3E4, 0x92D4B1, 100, 0);
    public PanelAppearance {
        width = Math.clamp(width, 220, 420); height = Math.clamp(height, 210, 360);
        background &= 0xFFFFFF; header &= 0xFFFFFF; border &= 0xFFFFFF;
        label &= 0xFFFFFF; value &= 0xFFFFFF; accent &= 0xFFFFFF;
        hudX = Math.clamp(hudX, 0, 100); hudY = Math.clamp(hudY, 0, 100);
    }
    public static int opaque(int rgb) { return 0xFF000000 | rgb; }
    private static final Pattern SECTION = Pattern.compile("(?ms)^\\[island_panel]\\s*\\R(.*?)(?=^\\[|\\z)");
    public static PanelAppearance load(Path path) throws IOException {
        if (Files.notExists(path)) return DEFAULT;
        var section = SECTION.matcher(Files.readString(path));
        if (!section.find()) return DEFAULT;
        String raw = section.group(1);
        var d = DEFAULT;
        return new PanelAppearance(read(raw, "width", d.width), read(raw, "height", d.height),
                read(raw, "background", d.background), read(raw, "header", d.header), read(raw, "border", d.border),
                read(raw, "label", d.label), read(raw, "value", d.value), read(raw, "accent", d.accent),
                read(raw, "hud_x", d.hudX), read(raw, "hud_y", d.hudY));
    }
    private static int read(String raw, String key, int fallback) {
        var match = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*(0x[0-9a-fA-F]+|[0-9]+)\\s*(?:#.*)?$").matcher(raw);
        try { return match.find() ? Integer.decode(match.group(1)) : fallback; }
        catch (NumberFormatException invalid) { return fallback; }
    }
    public void save(Path path) throws IOException {
        String raw = Files.exists(path) ? Files.readString(path) : "# Skyblock Multi client preferences\n";
        var match = SECTION.matcher(raw);
        boolean found = match.find();
        String section = found ? match.group(1) : "";
        Map<String, Integer> settings = new LinkedHashMap<>();
        settings.put("width", width); settings.put("height", height); settings.put("background", background);
        settings.put("header", header); settings.put("border", border); settings.put("label", label);
        settings.put("value", value); settings.put("accent", accent); settings.put("hud_x", hudX); settings.put("hud_y", hudY);
        for (var setting : settings.entrySet()) {
            var assignment = Pattern.compile("(?m)^\\s*" + setting.getKey() + "\\s*=[^\\r\\n]*").matcher(section);
            String replacement = setting.getKey() + " = " + setting.getValue();
            section = assignment.find() ? assignment.replaceFirst(replacement) : section.stripTrailing() + "\n" + replacement + "\n";
        }
        String updated = found ? raw.substring(0, match.start(1)) + section + raw.substring(match.end(1))
                : raw.stripTrailing() + "\n\n[island_panel]\n" + section;
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, updated);
        if (Files.exists(path)) Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
