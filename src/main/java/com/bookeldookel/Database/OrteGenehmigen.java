package com.bookeldookel.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class OrteGenehmigen {

    public static void pruefeOrte() {
        Connection conn = Datenbankmanager.getConnection();
        if (conn == null) {
            System.err.println("Keine Datenbankverbindung möglich. Abbruch der Ortsprüfung.");
            return;
        }

        // Suche nach allen Einträgen, bei denen zuordnung befüllt ist, aber zuordnung_check noch nicht 1 ist
        String selectSQL = "SELECT aufnahme_id, inhalt, zuordnung FROM tonaufnahmen " +
                           "WHERE zuordnung IS NOT NULL AND (zuordnung_check IS NULL OR zuordnung_check != 1)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {

            Scanner scanner = new Scanner(System.in);
            boolean eintraegeGefunden = false;

            System.out.println("\n=== Starte Prüfung der zugeordneten Orte ===");
            System.out.println("Bedienung:");
            System.out.println("- [Enter] drücken, um den zugeordneten Ort zu BESTÄTIGEN.");
            System.out.println("- 'x' oder 'X' eingeben, um den Eintrag UNVERÄNDERT zu lassen (wird übersprungen).");
            System.out.println("- 'quit' oder 'exit' eingeben, um die Prüfung vorzeitig ABZUBRECHEN.\n");

            while (rs.next()) {
                eintraegeGefunden = true;
                int id = rs.getInt("aufnahme_id");
                String inhalt = rs.getString("inhalt");
                String zuordnung = rs.getString("zuordnung");

                System.out.println("--------------------------------------------------");
                System.out.println("Artikeltext : " + inhalt);
                System.out.println("Zugeordneter Ort : " + zuordnung);
                System.out.print("Eingabe []: ");

                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    System.out.println("Prüfung vorzeitig abgebrochen.");
                    break;
                } else if (input.equalsIgnoreCase("x")) {
                    // Überspringen
                    System.out.println("-> Eintrag ÜBERSPRUNGEN.");
                } else if (input.isEmpty()) {
                    // Als geprüft markieren
                    markiereOrtAlsGeprueft(conn, id);
                    System.out.println("-> Ort BESTÄTIGT.");
                } else {
                    // Bei jeder anderen Eingabe gehen wir momentan davon aus, dass es übersprungen wird
                    // Da die Anforderung nichts über die Korrektur des Ortes durch Texteingabe sagt.
                    System.out.println("-> Ungültige Eingabe. Eintrag wird ÜBERSPRUNGEN.");
                }
            }

            if (!eintraegeGefunden) {
                System.out.println("Es gibt keine ungeprüften Ortszuordnungen.");
            }
            
            System.out.println("=== Ortsprüfung beendet ===\n");

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der ungeprüften Ortszuordnungen: " + e.getMessage());
        }
    }

    private static void markiereOrtAlsGeprueft(Connection conn, int id) {
        String updateSQL = "UPDATE tonaufnahmen SET zuordnung_check = 1 WHERE aufnahme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Aktualisieren der Ortsbestätigung für ID " + id + ": " + e.getMessage());
        }
    }
}
