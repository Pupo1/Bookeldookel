package com.bookeldookel.Database;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.stream.Stream;

public class Transkript {

    // Das Modell, das wir verwenden wollen
    private static final String MODEL_NAME = "gemini-3.1-flash-lite-preview";

    /**
     * Stellt sicher, dass die Datenbank-Tabelle existiert.
     */
    public static void initDatabase() {
        Datenbankmanager.erstelleTabelleFallsNichtExistent();
    }

    /**
     * Durchsucht den diyrecord-Ordner und initiiert die Transkription für alle .wav-Dateien.
     * Prüft vorab in der Datenbank, ob die Datei bereits erfolgreich transkribiert wurde.
     */
    public static void transkribiereVerzeichnis() {
        Path diyrecordDir = Paths.get("src", "main", "resources", "diyrecord");

        if (!Files.exists(diyrecordDir) || !Files.isDirectory(diyrecordDir)) {
            System.out.println("Der Ordner 'diyrecord' existiert nicht oder ist kein Verzeichnis.");
            return;
        }

        try (Stream<Path> paths = Files.list(diyrecordDir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                 .forEach(wavFile -> {
                     // Hole den Pfad als String wie er in der DB stehen würde
                     String pfadStr = wavFile.toAbsolutePath().toString();
                     if (pfadStr.length() > 100) pfadStr = pfadStr.substring(0, 100);

                     String vorhandenerInhalt = Datenbankmanager.holeVorhandeneTranskription(pfadStr);
                     
                     if (vorhandenerInhalt != null) {
                         // Datei existiert schon und check != 1 -> wird nicht neu transkribiert
                         System.out.println("Datei " + wavFile.getFileName() + " wurde bereits transkribiert. Überspringe API-Aufruf.");
                         System.out.println("  -> Vorhandener Text in DB: " + vorhandenerInhalt);
                     } else {
                         // Muss (neu) transkribiert werden
                         String ergebnis = transkribiere(wavFile);
                         System.out.println("Verarbeitung für " + wavFile.getFileName() + " abgeschlossen.");
                         
                         // Nur warten, wenn das Ergebnis kein Fehler war
                         if (ergebnis != null && !isErrorText(ergebnis)) {
                             try {
                                 // Warte 16 Sekunden (13 Sekunden + 3 Sekunden extra) für mehr Puffer
                                 Thread.sleep(16000); 
                             } catch (InterruptedException e) {
                                 Thread.currentThread().interrupt();
                             }
                         }
                     }
                 });
        } catch (IOException e) {
            System.err.println("Fehler beim Zugriff auf diyrecord: " + e.getMessage());
        }
    }

    /**
     * Transkribiert eine gegebene .wav-Audio-Datei in Text mithilfe der Google Gemini API
     * und speichert das Ergebnis in der Datenbank.
     *
     * @param audioFile Der Pfad zur .wav-Datei.
     * @return Der transkribierte Text oder eine Fehlermeldung.
     */
    private static String transkribiere(Path audioFile) {
        String pfadStr = audioFile.toAbsolutePath().toString();

        System.out.println("Starte Gemini-Transkription (" + MODEL_NAME + ") für: " + audioFile.getFileName() + " ...");

        // API Key aus der Umgebungsvariable lesen
        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
             System.err.println("FEHLER: Die Umgebungsvariable 'BOOKELDOOKEL_API_KEY' ist nicht gesetzt.");
             return "Fehler: API-Key fehlt.";
        }

        String transcribedText;
        boolean errorOccurred = false;

        try (Client client = Client.builder().apiKey(apiKey).build()) {

            // Audio-Datei einlesen und in Base64 umwandeln
            byte[] audioBytes = Files.readAllBytes(audioFile);
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            // Den Audio-Part erstellen (InlineData)
            Part audioPart = Part.builder()
                    .inlineData(com.google.genai.types.Blob.builder()
                            .mimeType("audio/wav")
                            .data(base64Audio)
                            .build())
                    .build();

            // Den Text-Prompt erstellen
            Part textPart = Part.builder()
                    .text("Bitte transkribiere diese Audioaufnahme so exakt wie möglich. Schreibe nur den erkannten Text, keine weiteren Kommentare.")
                    .build();

            // Den Content zusammenstellen
            Content content = Content.builder()
                    .parts(java.util.Arrays.asList(audioPart, textPart))
                    .build();

            // Die Anfrage an das Modell senden.
            GenerateContentResponse response = client.models.generateContent(
                MODEL_NAME, 
                content, 
                GenerateContentConfig.builder().build()
            );
            
            transcribedText = response.text();
            
            if (transcribedText != null) {
                transcribedText = transcribedText.trim();
                System.out.println("Transkription erfolgreich.");
            } else {
                transcribedText = "Kein Text erkannt.";
                errorOccurred = true;
            }

        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Audio-Datei: " + e.getMessage());
            transcribedText = "Fehler beim Lesen der Datei: " + e.getMessage();
            errorOccurred = true;
        } catch (Exception e) {
            System.err.println("Fehler bei der Kommunikation mit der Gemini API: " + e.getMessage());
            transcribedText = "Fehler bei der API-Anfrage: " + e.getMessage();
            errorOccurred = true;
        }

        // Wir prüfen, ob der Text nach einer Fehlermeldung aussieht oder ob ein Exception-Catch-Block durchlaufen wurde.
        // Falls ja, speichern wir die Transkription NICHT in der Datenbank, damit sie beim nächsten Start erneut versucht wird.
        if (errorOccurred || isErrorText(transcribedText)) {
            System.err.println("Transkription wird aufgrund eines Fehlers oder Quota-Limits NICHT in die Datenbank geschrieben.");
        } else {
            // In die Datenbank schreiben (oder aktualisieren)
            Datenbankmanager.speichereOderAktualisiereTranskription(pfadStr, transcribedText);
        }

        return transcribedText;
    }

    /**
     * Prüft, ob der zurückgegebene Text nach einer typischen Fehlermeldung aussieht (z.B. API Quota Exceeded).
     */
    private static boolean isErrorText(String text) {
        if (text == null) return true;
        
        String lowerText = text.toLowerCase();
        
        // Prüfe auf gängige Schlüsselwörter in Fehlermeldungen
        return lowerText.contains("error") || 
               lowerText.contains("fehler") || 
               lowerText.contains("429 too many requests") ||
               lowerText.contains("quota exceeded") ||
               lowerText.contains("rate limit") ||
               lowerText.contains("exception") ||
               lowerText.contains("404 not found") ||
               lowerText.contains("is not found for api version") ||
               lowerText.contains("is not supported for");
    }
}
