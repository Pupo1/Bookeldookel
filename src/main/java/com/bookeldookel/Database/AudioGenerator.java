package com.bookeldookel.Database;

import java.io.IOException;
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
import java.util.Arrays;
import java.util.List;

public class AudioGenerator {

    /**
     * Sammelt alle geprüften und genehmigten Einträge aus tonaufnahmen, hängt an die 
     * ursprüngliche .wav-Datei des Artikels die Audioaufnahme der Schachtelregistrierung an,
     * speichert sie im Ordner resources/tour und überträgt sie anschließend auf den Bookii-Stift, 
     * falls dieser noch/wieder angeschlossen ist.
     */
    public static void generiereUndUebertrageAudios() {
        System.out.println("\n=== Starte Audio-Kombination (AudioGenerator) ===");

        // Pfade getrennt nach Artikel und Ort
        Path archivArtikelDir = Paths.get("src", "main", "resources", "archivArtikel");
        Path archivOrteDir = Paths.get("src", "main", "resources", "archivOrte");
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

        // Wir holen alle Einträge aus tonaufnahmen, die erfolgreich einer Schachtel zugeordnet wurden
        // Hierbei ist "zuordnung" der Ort / die Bezeichnung der Schachtel.
        String selectSQL = "SELECT t.aufnahme_id, t.pfad as artikel_pfad, t.zuordnung " +
                           "FROM tonaufnahmen t " +
                           "WHERE t.zuordnung_check = 1 AND t.zuordnung IS NOT NULL";

        List<Path> erzeugteDateien = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {

            boolean eintraegeGefunden = false;

            while (rs.next()) {
                eintraegeGefunden = true;
                int id = rs.getInt("aufnahme_id");
                String artikelPfadStr = rs.getString("artikel_pfad");
                String zuordnung = rs.getString("zuordnung");

                // Wir extrahieren den reinen Dateinamen des Artikels, um ihn im archivArtikel zu suchen
                String dateiNameArtikel = extractFileName(artikelPfadStr);
                if (dateiNameArtikel == null || dateiNameArtikel.isEmpty()) {
                    System.err.println("Konnte Dateinamen für Artikel ID " + id + " nicht extrahieren.");
                    continue;
                }

                // Jetzt suchen wir nach der passenden Schachtel-Tondatei.
                String schachtelAudioPfad = findeSchachtelAudioPfad(conn, zuordnung);
                
                if (schachtelAudioPfad == null) {
                    System.err.println("WARNUNG: Keine Tondatei für Schachtel '" + zuordnung + "' gefunden. Überspringe Zusammenführung für " + dateiNameArtikel);
                    continue;
                }
                
                String dateiNameSchachtel = extractFileName(schachtelAudioPfad);

                Path artikelWav = archivArtikelDir.resolve(dateiNameArtikel);
                Path schachtelWav = archivOrteDir.resolve(dateiNameSchachtel);
                
                // Wir speichern die kombinierte Datei im tour Ordner mit dem Originalnamen der Artikel-Wav, 
                // da sie in Bookiiconnection / Bookii Stift exakt so (unter dieser OID) erwartet wird.
                Path zielDateiTour = tourDir.resolve(dateiNameArtikel);

                if (!Files.exists(artikelWav)) {
                    System.err.println("Artikel-Audio nicht gefunden im Archiv: " + artikelWav);
                    continue;
                }
                if (!Files.exists(schachtelWav)) {
                    System.err.println("Schachtel-Audio nicht gefunden im Archiv: " + schachtelWav);
                    continue;
                }

                System.out.println("Kombiniere: " + dateiNameArtikel + " + " + dateiNameSchachtel);

                // Einfaches binäres Aneinanderhängen von wav-Dateien 
                boolean erfolg = kombiniereWav(artikelWav, schachtelWav, zielDateiTour);

                if (erfolg) {
                    erzeugteDateien.add(zielDateiTour);
                    System.out.println("Audio erfolgreich gespeichert unter: " + zielDateiTour);
                } else {
                    System.err.println("Audio-Kombination fehlgeschlagen für ID " + id);
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

        System.out.println("\n=== Audio-Kombination beendet ===");
    }

    /**
     * Sucht in der Tabelle 'schachtel' nach dem Tondateipfad, der zu der Zuordnung (Ort oder Inhalt) passt.
     */
    private static String findeSchachtelAudioPfad(Connection conn, String zuordnung) {
        String selectSQL = "SELECT Tondateipfad FROM schachtel WHERE Ort = ? OR Inhalt = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, zuordnung);
            pstmt.setString(2, zuordnung);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Tondateipfad");
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Suchen des Schachtel-Audiopfads: " + e.getMessage());
        }
        return null;
    }

    /**
     * Kombiniert zwei .wav Dateien. 
     * Dies hängt einfach die Payload-Daten der zweiten Datei an die erste an und passt den Header an.
     * Setzt voraus, dass beide Dateien dasselbe Audio-Format haben.
     */
    private static boolean kombiniereWav(Path wav1, Path wav2, Path ziel) {
        try {
            byte[] file1Bytes = Files.readAllBytes(wav1);
            byte[] file2Bytes = Files.readAllBytes(wav2);
            
            // Sehr rudimentärer WAV-Concat:
            // WAV Header ist normalerweise 44 Bytes groß.
            if (file1Bytes.length < 44 || file2Bytes.length < 44) {
                System.err.println("Eine der WAV-Dateien ist zu klein (kein gültiger Header).");
                return false;
            }

            int payload2Length = file2Bytes.length - 44;
            int combinedLength = file1Bytes.length + payload2Length;

            byte[] combinedBytes = Arrays.copyOf(file1Bytes, combinedLength);
            System.arraycopy(file2Bytes, 44, combinedBytes, file1Bytes.length, payload2Length);

            // Datei-Größen im Header anpassen (Position 4 und 40)
            int dataSize = combinedLength - 44;
            int fileSize = combinedLength - 8;

            // fileSize in Bytes 4-7 (Little Endian)
            combinedBytes[4] = (byte) (fileSize & 0xFF);
            combinedBytes[5] = (byte) ((fileSize >> 8) & 0xFF);
            combinedBytes[6] = (byte) ((fileSize >> 16) & 0xFF);
            combinedBytes[7] = (byte) ((fileSize >> 24) & 0xFF);

            // dataSize in Bytes 40-43 (Little Endian)
            combinedBytes[40] = (byte) (dataSize & 0xFF);
            combinedBytes[41] = (byte) ((dataSize >> 8) & 0xFF);
            combinedBytes[42] = (byte) ((dataSize >> 16) & 0xFF);
            combinedBytes[43] = (byte) ((dataSize >> 24) & 0xFF);

            Files.write(ziel, combinedBytes);
            return true;

        } catch (IOException e) {
            System.err.println("Fehler beim Kombinieren der WAV-Dateien: " + e.getMessage());
            return false;
        }
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

        System.out.println("Alle kombinierten Dateien wurden auf den Stift übertragen.");
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
}
