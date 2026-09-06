package ipg.cooling.catalog;

import ipg.cooling.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Loads cooling-plate definitions from a JSON file on disk.
 * Search order: {@code -Dplates.file}, {@code LASER_COOLING_PLATES},
 * {@code ./data/plates.json}, {@code <jar>/data/plates.json}.
 * If the file is missing, the bundled resource is copied to {@code ./data/plates.json}.
 */
public final class PlateCatalog {
    public static final String FILE_NAME = "plates.json";
    private static final String RESOURCE = "/ipg/cooling/plates.json";

    private final Path source;
    private final List<PlateDefinition> plates;

    public PlateCatalog(Path source, List<PlateDefinition> plates) {
        this.source = source;
        this.plates = List.copyOf(plates);
    }

    public Path source() {
        return source;
    }

    public List<PlateDefinition> plates() {
        return plates;
    }

    public PlateDefinition byId(String id) {
        if (id == null) {
            return plates.isEmpty() ? null : plates.getFirst();
        }
        return plates.stream()
                .filter(plate -> id.equalsIgnoreCase(plate.id()))
                .findFirst()
                .orElse(plates.isEmpty() ? null : plates.getFirst());
    }

    public static PlateCatalog load() {
        return load(resolveCatalogFile());
    }

    public static PlateCatalog load(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            if (!Files.exists(path)) {
                extractBundled(path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return parse(path, reader);
            }
        } catch (IOException ex) {
            try (InputStream in = PlateCatalog.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    throw new IllegalArgumentException(I18n.t("error.catalog"), ex);
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return parse(path, reader);
                }
            } catch (IOException fallback) {
                throw new IllegalArgumentException(I18n.t("error.catalog"), fallback);
            }
        }
    }

    static PlateCatalog parse(Path source, Reader reader) {
        List<PlateDefinition> plates;
        try {
            plates = CatalogJson.plates(CatalogJson.read(reader));
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException(I18n.t("error.catalog"), ex);
        }
        if (plates.isEmpty()) {
            throw new IllegalArgumentException(I18n.t("error.catalogEmpty"));
        }
        for (PlateDefinition plate : plates) {
            if (plate.id() == null || plate.plate() == null || plate.tube() == null || plate.layout() == null) {
                throw new IllegalArgumentException(I18n.t("error.catalogEmpty"));
            }
        }
        return new PlateCatalog(source, plates);
    }

    public static Path resolveCatalogFile() {
        String property = System.getProperty("plates.file");
        if (property != null && !property.isBlank()) {
            return Path.of(property);
        }
        String env = System.getenv("LASER_COOLING_PLATES");
        if (env != null && !env.isBlank()) {
            return Path.of(env);
        }
        Path cwd = Path.of(System.getProperty("user.dir"), "data", FILE_NAME);
        if (Files.exists(cwd)) {
            return cwd;
        }
        Path jarDir = jarDirectory();
        if (jarDir != null) {
            Path beside = jarDir.resolve("data").resolve(FILE_NAME);
            if (Files.exists(beside)) {
                return beside;
            }
        }
        return cwd;
    }

    private static void extractBundled(Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = PlateCatalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("bundled " + RESOURCE + " is missing");
            }
            Files.copy(in, target);
        }
    }

    private static Path jarDirectory() {
        try {
            var location = PlateCatalog.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            Path path = Path.of(location.toURI());
            return Files.isDirectory(path) ? path : path.getParent();
        } catch (Exception ex) {
            return null;
        }
    }
}
