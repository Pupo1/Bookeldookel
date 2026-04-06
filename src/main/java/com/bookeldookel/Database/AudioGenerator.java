package com.bookeldookel.Database;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AudioGenerator {

    /**
     * Sammelt alle geprüften und genehmigten Einträge aus tonaufnahmen, generiert daraus
     * Text-to-Speech Audios (.wav), speichert sie im Ordner resources/tour und überträgt
     * sie anschließend auf den Bookii-Stift, falls dieser noch/wieder angeschlossen ist.
     */
    public static void generiereUndUebertrageAudios() {
        System.out.println("\n=== Starte Audio-Generierung (AudioGenerator) ===");

        Path tourDir = Paths.get("src", "main", "resources", "tour");
        try {
            if (!Files.exists(tourDir)) {
                Files.createDirectories(tourDir);
            }
        } catch (IOException e) {
            System.err.println("Konnte Ordner 'resources/tour' nicht erstellen: " + e.getMessage());
            return;
        }

        Connection conn = Datenbankmanager.getConnection();
        if (conn == null) {
            System.err.println("Keine Datenbankverbindung möglich.");
            return;
        }

        // Wir holen alle Einträge, die eine Zuordnung haben und geprüft wurden.
        String selectSQL = "SELECT aufnahme_id, pfad, inhalt, zuordnung FROM tonaufnahmen " +
                           "WHERE zuordnung_check = 1 AND zuordnung IS NOT NULL";

        List<Path> erzeugteDateien = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {

            boolean eintraegeGefunden = false;

            while (rs.next()) {
                eintraegeGefunden = true;
                int id = rs.getInt("aufnahme_id");
                String pfadStr = rs.getString("pfad");
                String inhalt = rs.getString("inhalt");
                String zuordnung = rs.getString("zuordnung");

                // Text zusammenbauen: Artikelname (Inhalt) + Zuordnung
                String zuSprechenderText = inhalt + ". " + zuordnung;
                System.out.println("\nGeneriere Audio für: \"" + zuSprechenderText + "\"");

                // Dateinamen aus dem ursprünglichen Pfad extrahieren
                String dateiName = extractFileName(pfadStr);
                if (dateiName == null || dateiName.isEmpty()) {
                    dateiName = "aufnahme_" + id + ".wav";
                }

                Path zielDateiTour = tourDir.resolve(dateiName);

                // Google Text-to-Speech aufrufen
                boolean erfolg = generiereWavMitGoogle(zuSprechenderText, zielDateiTour);

                if (erfolg) {
                    erzeugteDateien.add(zielDateiTour);
                    System.out.println("Audio erfolgreich gespeichert unter: " + zielDateiTour);
                } else {
                    System.err.println("Audio-Generierung fehlgeschlagen für ID " + id);
                }
            }

            if (!eintraegeGefunden) {
                System.out.println("Keine genehmigten Ortszuordnungen (zuordnung_check = 1) gefunden.");
            }

        } catch (SQLException e) {
            System.err.println("Datenbankfehler bei der Audio-Generierung: " + e.getMessage());
        }

        // Schritt b) Wenn Dateien erzeugt wurden, versuche sie auf den Stift zu übertragen
        if (!erzeugteDateien.isEmpty()) {
            uebertrageAufStift(erzeugteDateien);
        }

        System.out.println("\n=== Audio-Generierung beendet ===");
    }

    /**
     * Extrahiert den Dateinamen aus einem absoluten oder relativen Pfad.
     */
    private static String extractFileName(String pfad) {
        if (pfad == null) return null;
        // Für Windows und Linux Backslash oder Forwardslash behandeln
        int lastSlash = Math.max(pfad.lastIndexOf('/'), pfad.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < pfad.length() - 1) {
            return pfad.substring(lastSlash + 1);
        }
        return pfad;
    }

    /**
     * Sucht nach dem Laufwerk BOOKII_APP und kopiert die übergebenen Dateien in den diyrecord Ordner.
     */
    private static void uebertrageAufStift(List<Path> dateien) {
        System.out.println("\n=== Starte Übertragung auf Bookii-Stift ===");
        Path bookiiDrive = null;

        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            try {
                FileStore store = Files.getFileStore(root);
                if ("BOOKII_APP".equalsIgnoreCase(store.name())) {
                    bookiiDrive = root;
                    break;
                }
            } catch (IOException e) {
                // ignorieren
            }
        }

        if (bookiiDrive == null) {
            System.out.println("Der Bookii-Stift (Laufwerk BOOKII_APP) ist aktuell nicht angeschlossen.");
            System.out.println("Die Dateien liegen im Ordner 'resources/tour' und können später manuell kopiert werden.");
            return;
        }

        Path zielVerzeichnisStift = bookiiDrive.resolve("diyrecord");

        // Wenn der Ordner auf dem Stift nicht existiert, legen wir ihn an (oder brechen ab)
        if (!Files.exists(zielVerzeichnisStift)) {
            try {
                Files.createDirectories(zielVerzeichnisStift);
                System.out.println("Ordner 'diyrecord' auf dem Stift erstellt.");
            } catch (IOException e) {
                System.err.println("Konnte 'diyrecord' auf dem Stift nicht erstellen: " + e.getMessage());
                return;
            }
        }

        for (Path datei : dateien) {
            Path zielDateiAufStift = zielVerzeichnisStift.resolve(datei.getFileName());
            try {
                Files.copy(datei, zielDateiAufStift, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Übertragen: " + datei.getFileName());
            } catch (IOException e) {
                System.err.println("Fehler beim Übertragen von " + datei.getFileName() + ": " + e.getMessage());
            }
        }

        System.out.println("Alle generierten Dateien wurden auf den Stift übertragen.");
    }

    /**
     * Nutzt die Google Cloud Text-to-Speech API, um Text in eine .wav Datei umzuwandeln.
     * Voraussetzung: Die Umgebungsvariable GOOGLE_APPLICATION_CREDENTIALS muss gesetzt sein,
     * da die TTS API standardmäßig Service Accounts erwartet.
     */
    private static boolean generiereWavMitGoogle(String text, Path zielPfad) {
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

            // Stimme auswählen (z.B. Deutsch, weiblich/männlich, Wavenet)
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("de-DE")
                    .setName("de-DE-Wavenet-C") // Eine hochwertige deutsche Stimme
                    .build();

            // Audio-Format auf LINEAR16 (.wav) setzen
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16)
                    // Optional: Sample Rate anpassen, falls Bookii ein bestimmtes Format erwartet (z.B. 44100 oder 22050)
                    // .setSampleRateHertz(44100) 
                    .build();

            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContents = response.getAudioContent();

            try (OutputStream out = new FileOutputStream(zielPfad.toFile())) {
                out.write(audioContents.toByteArray());
                return true;
            }

        } catch (Exception e) {
            System.err.println("Fehler bei der Google Text-to-Speech Anfrage: " + e.getMessage());
            System.err.println("HINWEIS: Stelle sicher, dass die Dependency 'com.google.cloud:google-cloud-texttospeech' in build.gradle.kts eingetragen ist");
            System.err.println("         und GOOGLE_APPLICATION_CREDENTIALS auf ein valides Service Account JSON verweist.");
            return false;
        }
    }
}
