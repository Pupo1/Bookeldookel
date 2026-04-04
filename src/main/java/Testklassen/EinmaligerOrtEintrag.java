package Testklassen;

import com.bookeldookel.Database.Datenbankmanager;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EinmaligerOrtEintrag {

    private static final String MODEL_NAME = "gemma-3-12b-it";

    public static void main(String[] args) {
        System.out.println("\n=== Starte Verarbeitung einer einzelnen .wav-Datei aus dem mp3-Ordner (Testklasse) ===");

        Path mp3Dir = Paths.get("src", "main", "resources", "mp3");
        Path archivDir = Paths.get("src", "main", "resources", "archiv");

        if (!Files.exists(mp3Dir) || !Files.isDirectory(mp3Dir)) {
            System.out.println("Der Ordner 'mp3' existiert nicht. Abbruch.");
            return;
        }

        try {
            if (!Files.exists(archivDir)) {
                Files.createDirectories(archivDir);
            }
        } catch (IOException e) {
            System.err.println("Konnte Archiv-Ordner nicht erstellen: " + e.getMessage());
            return;
        }

        try (Stream<Path> paths = Files.list(mp3Dir)) {
            Optional<Path> firstWavOpt = paths
                    .filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                    .findFirst();

            if (!firstWavOpt.isPresent()) {
                System.out.println("Keine .wav-Dateien im Ordner 'mp3' gefunden.");
                return;
            }

            Path wavFile = firstWavOpt.get();
            System.out.println("\nVerarbeite erste gefundene Datei: " + wavFile.getFileName());

            // 1. Transkribieren
            String transkription = transkribiere(wavFile);

            if (transkription == null || isErrorText(transkription)) {
                System.out.println("Transkription fehlgeschlagen oder ungültig. Abbruch.");
                return;
            }

            System.out.println("Erkannter Text: " + transkription);

            // 2. Prüfen, ob eine Zahl am Anfang steht
            Pattern pattern = Pattern.compile("^\\D*(\\d+)");
            Matcher matcher = pattern.matcher(transkription);

            if (matcher.find()) {
                String zahlStr = matcher.group(1);
                int nummer = Integer.parseInt(zahlStr);

                // Den Rest des Textes nach der Zahl extrahieren
                int endeIndexZahl = matcher.end(1);
                String restText = transkription.substring(endeIndexZahl).trim();

                System.out.println("Nummer gefunden: " + nummer);
                System.out.println("Restlicher Text: " + restText);

                // 3. In Datenbank prüfen / eintragen
                // Initialisiere die Datenbankverbindung und -struktur, falls noch nicht geschehen
                Datenbankmanager.erstelleTabelleFallsNichtExistent();
                
                verarbeiteDatenbank(nummer, restText, wavFile, archivDir);

            } else {
                System.err.println("FEHLER: Keine Zahl am Anfang des Textes gefunden. Datei wird ignoriert.");
            }

        } catch (IOException e) {
            System.err.println("Fehler beim Zugriff auf mp3-Ordner: " + e.getMessage());
        }

        System.out.println("\n=== Verarbeitung beendet ===");
    }

    private static void verarbeiteDatenbank(int nummer, String text, Path originalDatei, Path archivDir) {
        String ort = Datenbankmanager.getOrtFuerSchachtel(nummer);

        try {
            if ("NOT_FOUND".equals(ort)) {
                // Schachtel existiert noch nicht -> Neu anlegen mit Ort
                Path zielPfad = findeEindeutigenZielpfad(archivDir, originalDatei.getFileName().toString());
                String tondateipfad = zielPfad.toAbsolutePath().toString().replace("\\", "/");

                Datenbankmanager.neueSchachtelAnlegen(nummer, text, tondateipfad);

                // Datei ins Archiv verschieben
                Files.move(originalDatei, zielPfad, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Datei archiviert nach: " + zielPfad);

            } else if (ort == null || ort.trim().isEmpty()) {
                // Schachtel existiert, aber Spalte Ort ist noch leer.
                System.out.println("Aktualisiere Ort für Schachtel " + nummer + ".");

                Path zielPfad = findeEindeutigenZielpfad(archivDir, originalDatei.getFileName().toString());
                String tondateipfad = zielPfad.toAbsolutePath().toString().replace("\\", "/");

                Datenbankmanager.updateSchachtelOrt(nummer, text, tondateipfad);

                // Datei ins Archiv verschieben
                Files.move(originalDatei, zielPfad, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Datei archiviert nach: " + zielPfad);

            } else {
                // Ort existiert bereits -> Inhalt hinzufügen
                System.out.println("Schachtel " + nummer + " hat bereits den Ort: '" + ort + "'. Füge Inhalt hinzu.");
                Datenbankmanager.inhaltZuSchachtelHinzufuegen(nummer, text);

                // Da es sich um Inhalt handelte, wird der Dateiname NICHT in die DB eingetragen.
                Path zielPfad = findeEindeutigenZielpfad(archivDir, originalDatei.getFileName().toString());
                Files.move(originalDatei, zielPfad, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Datei (Inhalt) archiviert nach: " + zielPfad);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Verschieben der Datei ins Archiv: " + e.getMessage());
        }
    }

    private static Path findeEindeutigenZielpfad(Path archivDir, String dateiName) {
        Path zielPfad = archivDir.resolve(dateiName);
        if (!Files.exists(zielPfad)) {
            return zielPfad;
        }

        String basisName = dateiName;
        String erweiterung = "";
        int letzterPunkt = dateiName.lastIndexOf('.');
        if (letzterPunkt > 0) {
            basisName = dateiName.substring(0, letzterPunkt);
            erweiterung = dateiName.substring(letzterPunkt);
        }

        int zaehler = 1;
        while (Files.exists(zielPfad)) {
            String neuerName = basisName + "_" + zaehler + erweiterung;
            zielPfad = archivDir.resolve(neuerName);
            zaehler++;
        }
        return zielPfad;
    }

    private static String transkribiere(Path audioFile) {
        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("FEHLER: API Key fehlt.");
            return null;
        }

        try (Client client = Client.builder().apiKey(apiKey).build()) {
            byte[] audioBytes = Files.readAllBytes(audioFile);
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            Part audioPart = Part.builder()
                    .inlineData(com.google.genai.types.Blob.builder()
                            .mimeType("audio/wav")
                            .data(base64Audio)
                            .build())
                    .build();

            Part textPart = Part.builder()
                    .text("Bitte transkribiere diese Audioaufnahme exakt. Schreibe alle Zahlen als Ziffern (1016, 50, etc.). Keine Kommentare.")
                    .build();

            Content content = Content.builder()
                    .parts(Arrays.asList(audioPart, textPart))
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME,
                    content,
                    GenerateContentConfig.builder().build()
            );

            return response.text() != null ? response.text().trim() : null;

        } catch (Exception e) {
            System.err.println("Fehler bei der KI-Anfrage: " + e.getMessage());
            return null;
        }
    }

    private static boolean isErrorText(String text) {
        if (text == null) return true;
        String lowerText = text.toLowerCase();
        return lowerText.contains("error") ||
                lowerText.contains("fehler") ||
                lowerText.contains("429 too many requests") ||
                lowerText.contains("quota exceeded") ||
                lowerText.contains("rate limit");
    }
}
