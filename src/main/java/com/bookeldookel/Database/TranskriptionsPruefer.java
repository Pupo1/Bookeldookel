package com.bookeldookel.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

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

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der ungeprüften Transkriptionen: " + e.getMessage());
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
}
