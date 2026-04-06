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
                           "WHERE (zuordnung_check IS NULL OR zuordnung_check != 1)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {

            Scanner scanner = new Scanner(System.in);
            boolean eintraegeGefunden = false;

            System.out.println("\n=== Starte Prüfung der zugeordneten Orte ===");
            System.out.println("Bedienung:");
            System.out.println("- [Enter] drücken, um den zugeordneten Ort zu BESTÄTIGEN.");
            System.out.println("- 'x' oder 'X' eingeben, um den Eintrag UNVERÄNDERT zu lassen (wird übersprungen).");
            System.out.println("- 'quit' oder 'exit' eingeben, um die Prüfung vorzeitig ABZUBRECHEN.");
            System.out.println("- Eine Zahl gefolgt von Text eingeben (z.B. '1021 Silikon') um die Schachtel-ID und Zuordnung direkt zu setzen.\n");

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
                    // Prüfen ob die Eingabe mit einer Nummer beginnt (z.B. "1021 Silikon")
                    String trimmedInput = input.trim();
                    if (trimmedInput.length() > 0 && Character.isDigit(trimmedInput.charAt(0))) {
                        // Finde die Stelle wo die Zahl endet (erster Nicht-Ziffer-Charakter)
                        int i = 0;
                        while (i < trimmedInput.length() && Character.isDigit(trimmedInput.charAt(i))) {
                            i++;
                        }
                        
                        // Extrahiere die Nummer und den Rest
                        String nummerStr = trimmedInput.substring(0, i);
                        String restText = trimmedInput.substring(i).trim();
                        
                        try {
                            int schachtelId = Integer.parseInt(nummerStr);
                            // Update database: schachtel_id and zuordnung
                            aktualisiereOrtMitSchachtelId(conn, id, schachtelId, restText);
                            System.out.println("-> Ort gespeichert: schachtel_id=" + schachtelId + ", zuordnung='" + restText + "'");
                        } catch (NumberFormatException e) {
                            System.out.println("-> Ungültige Zahl. Eintrag wird ÜBERSPRUNGEN.");
                        }
                    } else {
                        // Bei jeder anderen Eingabe gehen wir momentan davon aus, dass es übersprungen wird
                        // Da die Anforderung nichts über die Korrektur des Ortes durch Texteingabe sagt.
                        System.out.println("-> Ungültige Eingabe. Eintrag wird ÜBERSPRUNGEN.");
                    }
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

    private static void aktualisiereOrtMitSchachtelId(Connection conn, int id, int schachtelId, String zuordnung) {
        String updateSQL = "UPDATE tonaufnahmen SET schachtel_id = ?, zuordnung = ?, zuordnung_check = 1 WHERE aufnahme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setInt(1, schachtelId);
            pstmt.setString(2, zuordnung);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Aktualisieren der Schachtelzuordnung für ID " + id + ": " + e.getMessage());
        }
    }
}
