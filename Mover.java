import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class Mover {
    public static void main(String[] args) {
        Path resourcesDir = Paths.get("src", "main", "resources");
        Path targetDir = resourcesDir.resolve("diyrecord");
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            if (Files.exists(resourcesDir)) {
                try (Stream<Path> paths = Files.list(resourcesDir)) {
                    paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                         .forEach(source -> {
                             try {
                                 Path target = targetDir.resolve(source.getFileName());
                                 Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                                 System.out.println("Verschoben: " + source.getFileName());
                             } catch (IOException e) {
                                 System.err.println("Fehler beim Verschieben von " + source.getFileName() + ": " + e.getMessage());
                             }
                         });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}