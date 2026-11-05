package cl.treecs.skyblockmulti.addon;

import java.nio.file.*;
import java.util.Properties;

/** Fail closed on unreadable state; retain the last file before atomic replacement. */
public final class RewardFile {
    private RewardFile() {}
    public static Properties read(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) { properties.load(reader); }
            catch (Exception error) { throw new IllegalStateException("Cannot read " + path, error); }
        }
        return properties;
    }
    public static void write(Path path, Properties properties) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(temporary)) { properties.store(writer, "Skyblock Multi rewards"); }
            if (Files.exists(path)) Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception error) { throw new IllegalStateException("Cannot save " + path, error); }
    }
}
