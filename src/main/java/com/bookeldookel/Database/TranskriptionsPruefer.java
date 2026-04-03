package com.bookeldookel.Database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.stream.Stream;

public class TranskriptionsPruefer {

    public static void pruefeTranskriptionen() {
        Connection conn = Datenbankmanager.getConnection();
        if (conn == null) {
            System.err.println("Keine Datenbankverbindung möglich. Abbruch der Prüfung.");
            return;
        }

        // Suche nach allen Einträgen, bei denen check_status nicht 1 ist
        String selectSQL = "SELECT aufnahme_id, pfad, inhalt FROM tonaufnahmen WHERE check_status IS NULL OR check_status != 1";
        
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {

            Scanner scanner = new Scanner(System.in);
            boolean eintraegeGefunden = false;

            System.out.println("\n=== Starte Prüfung der Transkriptionen ===");
            System.out.println("Bedienung:");
            System.out.println("- [Enter] drücken, um den Text als KORREKT (geprüft) zu markieren.");
            System.out.println("- 'x' eingeben, um den Eintrag UNVERÄNDERT zu lassen (wird übersprungen).");
            System.out.println("- 'lö' eingeben, um den Eintrag aus der Datenbank zu LÖSCHEN.");
            System.out.println("- Anderen Text eingeben, um den erkannten Text zu KORRIGIEREN.\n");

            while (rs.next()) {
                eintraegeGefunden = true;
                int id = rs.getInt("aufnahme_id");
                String pfad = rs.getString("pfad");
                String inhalt = rs.getString("inhalt");

                // Dateinamen aus dem Pfad extrahieren für eine übersichtlichere Anzeige
                String dateiName = pfad;
                int lastSlash = Math.max(pfad.lastIndexOf('/'), pfad.lastIndexOf('\\'));
                if (lastSlash >= 0) {
                    dateiName = pfad.substring(lastSlash + 1);
                }

                System.out.println("--------------------------------------------------");
                System.out.println("Datei : " + dateiName);
                System.out.println("Text  : " + inhalt);
                System.out.print("Eingabe []: ");

                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("x")) {
                    // Überspringen
                    System.out.println("-> Eintrag ÜBERSPRUNGEN.");
                } else if (input.equalsIgnoreCase("lö")) {
                    // Eintrag aus der Datenbank löschen
                    loescheEintrag(conn, id);
                    System.out.println("-> Eintrag GELÖSCHT.");
                } else if (input.isEmpty()) {
                    // Als geprüft markieren (mit altem Inhalt)
                    markiereAlsGeprueft(conn, id, inhalt);
                    System.out.println("-> Eintrag als KORREKT markiert.");
                } else {
                    // Text korrigieren und als geprüft markieren
                    markiereAlsGeprueft(conn, id, input);
                    System.out.println("-> Eintrag KORRIGIERT und als geprüft markiert.");
                }
            }

            if (!eintraegeGefunden) {
                System.out.println("Es gibt keine ungeprüften Transkriptionen.");
            }
            
            System.out.println("=== Prüfung beendet ===\n");

            // Nach Abschluss der Prüfung: Fragen, ob .wav-Dateien gelöscht/archiviert werden sollen
            frageNachWavLoeschung(scanner);

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der ungeprüften Transkriptionen: " + e.getMessage());
        }
    }

    /**
     * Fragt den Benutzer, ob alle .wav-Dateien im diyrecord-Ordner gelöscht/archiviert werden sollen.
     * Im DEVELOPER_MODUS werden die Dateien archiviert, sonst gelöscht.
     */
    private static void frageNachWavLoeschung(Scanner scanner) {
        Path diyrecordDir = Paths.get("src", "main", "resources", "diyrecord");
        
        if (!Files.exists(diyrecordDir) || !Files.isDirectory(diyrecordDir)) {
            return;
        }

        // Zähle .wav-Dateien
        long wavCount;
        try (Stream<Path> paths = Files.list(diyrecordDir)) {
            wavCount = paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p)).count();
        } catch (IOException e) {
            System.err.println("Fehler beim Zählen der .wav-Dateien: " + e.getMessage());
            return;
        }

        if (wavCount == 0) {
            System.out.println("Keine .wav-Dateien im diyrecord-Ordner vorhanden.");
            return;
        }

        String aktion = Main.DEVELOPER_MODUS ? "archiviert" : "gelöscht";
        System.out.println("--------------------------------------------------");
        System.out.println("Es befinden sich " + wavCount + " .wav-Datei(en) im diyrecord-Ordner.");
        System.out.print("Sollen alle .wav-Dateien " + aktion + " werden? (ja/nein): ");
        
        String antwort = scanner.nextLine().trim();
        
        if (antwort.equalsIgnoreCase("ja")) {
            if (Main.DEVELOPER_MODUS) {
                archiviereWavDateien(diyrecordDir);
            } else {
                loescheWavDateien(diyrecordDir);
            }
        } else {
            System.out.println("-> .wav-Dateien werden NICHT " + aktion + ".");
        }
    }

    /**
     * Archiviert alle .wav-Dateien aus dem diyrecord-Ordner in den archiv-Ordner.
     * Bei Namenskonflikten wird die Datei umbenannt.
     */
    private static void archiviereWavDateien(Path diyrecordDir) {
        Path archivDir = Paths.get("src", "main", "resources", "archiv");
        
        // Archiv-Ordner erstellen, falls nicht vorhanden
        try {
            if (!Files.exists(archivDir)) {
                Files.createDirectories(archivDir);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Erstellen des Archiv-Ordners: " + e.getMessage());
            return;
        }

        try (Stream<Path> paths = Files.list(diyrecordDir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                 .forEach(wavFile -> {
                     try {
                         Path zielPfad = findeEindeutigenZielpfad(archivDir, wavFile.getFileName().toString());
                         Files.move(wavFile, zielPfad, StandardCopyOption.REPLACE_EXISTING);
                         System.out.println("Archiviert: " + wavFile.getFileName() + " -> " + zielPfad.getFileName());
                     } catch (IOException e) {
                         System.err.println("Fehler beim Archivieren von " + wavFile.getFileName() + ": " + e.getMessage());
                     }
                 });
            System.out.println("-> Archivierung abgeschlossen.");
        } catch (IOException e) {
            System.err.println("Fehler beim Zugriff auf diyrecord: " + e.getMessage());
        }
    }

    /**
     * Findet einen eindeutigen Zielpfad im Archiv-Ordner.
     * Falls die Datei bereits existiert, wird ein Zähler angehängt.
     */
    private static Path findeEindeutigenZielpfad(Path archivDir, String dateiName) {
        Path zielPfad = archivDir.resolve(dateiName);
        
        if (!Files.exists(zielPfad)) {
            return zielPfad;
        }

        // Dateiname und Erweiterung trennen
        String basisName = dateiName;
        String erweiterung = "";
        int letzterPunkt = dateiName.lastIndexOf('.');
        if (letzterPunkt > 0) {
            basisName = dateiName.substring(0, letzterPunkt);
            erweiterung = dateiName.substring(letzterPunkt);
        }

        // Zähler hochzählen, bis ein freier Name gefunden wird
        int zaehler = 1;
        while (Files.exists(zielPfad)) {
            String neuerName = basisName + "_" + zaehler + erweiterung;
            zielPfad = archivDir.resolve(neuerName);
            zaehler++;
        }

        return zielPfad;
    }

    /**
     * Löscht alle .wav-Dateien aus dem diyrecord-Ordner.
     */
    private static void loescheWavDateien(Path diyrecordDir) {
        try (Stream<Path> paths = Files.list(diyrecordDir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                 .forEach(wavFile -> {
                     try {
                         Files.delete(wavFile);
                         System.out.println("Gelöscht: " + wavFile.getFileName());
                     } catch (IOException e) {
                         System.err.println("Fehler beim Löschen von " + wavFile.getFileName() + ": " + e.getMessage());
                     }
                 });
            System.out.println("-> Löschung abgeschlossen.");
        } catch (IOException e) {
            System.err.println("Fehler beim Zugriff auf diyrecord: " + e.getMessage());
        }
    }

    private static void markiereAlsGeprueft(Connection conn, int id, String neuerInhalt) {
        String updateSQL = "UPDATE tonaufnahmen SET inhalt = ?, check_status = 1 WHERE aufnahme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            // Kürzen, falls der Benutzer zu viel Text eingegeben hat
            if (neuerInhalt != null && neuerInhalt.length() > 500) {
                neuerInhalt = neuerInhalt.substring(0, 500);
            }
            
            pstmt.setString(1, neuerInhalt);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Aktualisieren des Eintrags ID " + id + ": " + e.getMessage());
        }
    }

    /**
     * Löscht einen Eintrag vollständig aus der Datenbank.
     * @param conn Die Datenbankverbindung.
     * @param id Die ID des zu löschenden Eintrags.
     */
    private static void loescheEintrag(Connection conn, int id) {
        String deleteSQL = "DELETE FROM tonaufnahmen WHERE aufnahme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            pstmt.setInt(1, id);
            int betroffeneZeilen = pstmt.executeUpdate();
            if (betroffeneZeilen == 0) {
                System.err.println("Warnung: Kein Eintrag mit ID " + id + " gefunden.");
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen des Eintrags ID " + id + ": " + e.getMessage());
        }
    }
}
