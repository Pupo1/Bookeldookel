package com.bookeldookel.Database;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class Transkript {

    // Das Modell, das wir verwenden wollen
    // Wichtig: Gemini 2.5 Flash heißt "gemini-2.5-flash" 
    private static final String MODEL_NAME = "gemini-2.5-flash"; 

    /**
     * Stellt sicher, dass die Datenbank-Tabelle existiert.
     */
    public static void initDatabase() {
        Datenbankmanager.erstelleTabelleFallsNichtExistent();
    }

    /**
     * Transkribiert eine gegebene .wav-Audio-Datei in Text mithilfe der Google Gemini API
     * und speichert das Ergebnis in der Datenbank.
     *
     * @param audioFile Der Pfad zur .wav-Datei.
     * @return Der transkribierte Text oder eine Fehlermeldung.
     */
    public static String transkribiere(Path audioFile) {
        System.out.println("Starte Gemini-Transkription für: " + audioFile.getFileName() + " ...");

        // API Key aus der Umgebungsvariable lesen

        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
             System.err.println("FEHLER: Die Umgebungsvariable 'BOOKELDOOKEL_API_KEY' ist nicht gesetzt.");
             return "Fehler: API-Key fehlt.";
        }

        String transcribedText = null;

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
            }

        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Audio-Datei: " + e.getMessage());
            transcribedText = "Fehler beim Lesen der Datei: " + e.getMessage();
        } catch (Exception e) {
            System.err.println("Fehler bei der Kommunikation mit der Gemini API: " + e.getMessage());
            transcribedText = "Fehler bei der API-Anfrage: " + e.getMessage();
        }

        // In die Datenbank schreiben
        Datenbankmanager.speichereTranskription(audioFile.toAbsolutePath().toString(), transcribedText);

        return transcribedText;
    }
}
