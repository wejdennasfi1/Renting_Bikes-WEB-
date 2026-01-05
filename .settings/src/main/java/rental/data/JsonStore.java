package rental.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class JsonStore {

    public static String readAll(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) return null;
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    public static void writeAll(String path, String content) throws IOException {
        Path p = Paths.get(path);
        if (p.getParent() != null) Files.createDirectories(p.getParent());
        Files.writeString(
                p, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    public static String unesc(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
