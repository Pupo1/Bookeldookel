package Testklassen;

import com.bookeldookel.Database.Transkript;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.stream.Stream;

public class Transkriptionstest {
    public static void main(String[] args) {
        Path diyrecordDir = Paths.get("src", "main", "resources", "diyrecord");

        if (!Files.exists(diyrecordDir) || !Files.isDirectory(diyrecordDir)) {
            System.out.println("Der Ordner 'diyrecord' existiert noch nicht oder ist leer.");
            return;
        }

        try (Stream<Path> paths = Files.list(diyrecordDir)) {
            // Die erste gefundene .wav-Datei ermitteln
            Optional<Path> firstWavFile = paths
                    .filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                    .findFirst();

            if (firstWavFile.isPresent()) {
                Path wavFile = firstWavFile.get();
                System.out.println("Erste .wav-Datei gefunden: " + wavFile.getFileName());

                // Transkription aufrufen
                String ergebnisText = Transkript.transkribiere(wavFile);

                System.out.println("\nTranskriptionsergebnis:");
                System.out.println("-------------------------------------------------");
                System.out.println(ergebnisText);
                System.out.println("-------------------------------------------------");
            } else {
                System.out.println("Keine .wav-Datei im Ordner 'diyrecord' gefunden.");
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Zugriff auf den Ordner: " + e.getMessage());
        }
    }
}
