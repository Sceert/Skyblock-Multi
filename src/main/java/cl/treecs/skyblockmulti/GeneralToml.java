package cl.treecs.skyblockmulti;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal TOML access for the small, fixed general server configuration schema. */
final class GeneralToml {
    private GeneralToml() {
    }

    static int readInteger(Path path, String key, int fallback) throws IOException {
        if (path == null || Files.notExists(path)) return fallback;
        Matcher matcher = assignment(key, "(-?\\d+)").matcher(
                Files.readString(path, StandardCharsets.UTF_8));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    static void writeInteger(Path path, String section, String key, int value) throws IOException {
        String raw = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        Pattern pattern = assignment(key, "-?\\d+");
        Matcher matcher = pattern.matcher(raw);
        String replacement = key + " = " + value;
        if (matcher.find()) {
            raw = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        } else {
            Pattern table = Pattern.compile("(?m)^\\s*\\[" + Pattern.quote(section) + "]\\s*$");
            Matcher tableMatcher = table.matcher(raw);
            if (tableMatcher.find()) {
                int insertAt = raw.length();
                Matcher nextTable = Pattern.compile("(?m)^\\s*\\[[^]]+]\\s*$")
                        .matcher(raw.substring(tableMatcher.end()));
                if (nextTable.find()) insertAt = tableMatcher.end() + nextTable.start();
                raw = raw.substring(0, insertAt) + replacement + System.lineSeparator()
                        + raw.substring(insertAt);
            } else {
                raw = raw.stripTrailing() + System.lineSeparator() + System.lineSeparator()
                        + "[" + section + "]" + System.lineSeparator() + replacement
                        + System.lineSeparator();
            }
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, raw, StandardCharsets.UTF_8);
    }

    private static Pattern assignment(String key, String valuePattern) {
        return Pattern.compile("(?m)^\\s*" + Pattern.quote(key)
                + "\\s*=\\s*" + valuePattern + "\\s*(?:#.*)?$");
    }
}
