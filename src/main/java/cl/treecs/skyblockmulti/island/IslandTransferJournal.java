package cl.treecs.skyblockmulti.island;

import cl.treecs.skyblockmulti.api.party.IslandTransferCoordinator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Commit decisions survive world-save timing; corruption is fail-closed. */
public final class IslandTransferJournal implements IslandTransferCoordinator.Journal {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private record Data(int schema, List<IslandTransferCoordinator.Decision> decisions) { }

    public IslandTransferJournal(Path path) { this.path = path; }

    @Override public List<IslandTransferCoordinator.Decision> load() throws IOException {
        if (Files.notExists(path)) return List.of();
        try {
            Data data = GSON.fromJson(Files.readString(path), Data.class);
            if (data == null || data.schema() != 1 || data.decisions() == null) {
                throw new IOException("Invalid island transfer journal: " + path);
            }
            return List.copyOf(data.decisions());
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid island transfer journal: " + path, invalid);
        }
    }

    @Override public void save(List<IslandTransferCoordinator.Decision> decisions) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        byte[] bytes = GSON.toJson(new Data(1, decisions)).getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        if (Files.exists(path)) {
            Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        // No non-atomic fallback: do not risk a partial commit decision.
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
