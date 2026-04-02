package com.bookeldookel.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Datenbankmanager {
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Umgebungsvariablen auslesen
                String ip = System.getenv("MARIADB_IP");
                String password = System.getenv("MARIADBPASSWORTAUFRAEUMER");
                String user = "Aufraeumer";
                String dbName = "wo";

                if (ip == null || ip.isEmpty() || password == null || password.isEmpty()) {
                    System.err.println("FEHLER: Datenbank-Umgebungsvariablen (MARIADB_IP, MARIADBPASSWORTAUFRAEUMER) sind nicht gesetzt.");
                    return null;
                }

                // JDBC URL zusammenbauen
                String url = "jdbc:mariadb://" + ip + ":3306/" + dbName;

                // Verbindung herstellen
                connection = DriverManager.getConnection(url, user, password);
                System.out.println("Datenbankverbindung zu 'wo' erfolgreich hergestellt.");

            } catch (SQLException e) {
                System.err.println("Datenbankverbindungsfehler: " + e.getMessage());
            }
        }
        return connection;
    }

    public static void schliesseVerbindung() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Datenbankverbindung geschlossen.");
            } catch (SQLException e) {
                System.err.println("Fehler beim Schließen der Datenbankverbindung: " + e.getMessage());
            }
        }
    }

    public static void erstelleTabelleFallsNichtExistent() {
        Connection conn = getConnection();
        if (conn == null) return;

        String createTableSQL = "CREATE TABLE IF NOT EXISTS tonaufnahmen ("
                + "aufnahme_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "pfad VARCHAR(100), "
                + "inhalt VARCHAR(500), "
                + "check_status TINYINT(1) DEFAULT NULL, "
                + "uebertragen TINYINT(1) DEFAULT NULL"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Tabelle 'tonaufnahmen' geprüft/erstellt.");
        } catch (SQLException e) {
            System.err.println("Fehler beim Erstellen der Tabelle: " + e.getMessage());
        }
    }

    public static void speichereTranskription(String pfad, String inhalt) {
        Connection conn = getConnection();
        if (conn == null) return;

        String insertSQL = "INSERT INTO tonaufnahmen (pfad, inhalt) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            // Kürzen, falls der Pfad oder Inhalt zu lang sind für die DB-Spalten
            if (pfad.length() > 100) pfad = pfad.substring(0, 100);
            if (inhalt != null && inhalt.length() > 500) inhalt = inhalt.substring(0, 500);

            pstmt.setString(1, pfad);
            pstmt.setString(2, inhalt);
            
            pstmt.executeUpdate();
            System.out.println("Transkription in Datenbank gespeichert: " + pfad);
        } catch (SQLException e) {
            System.err.println("Fehler beim Speichern in der Datenbank: " + e.getMessage());
        }
    }

    /**
     * Prüft, ob eine Transkription für den gegebenen Pfad existiert.
     * Wenn der check_status NICHT 1 (true) ist, wird der inhalt zurückgegeben
     * (als Signal, dass nicht neu transkribiert werden muss).
     * Ist der Datensatz nicht da oder check_status == 1, wird null zurückgegeben.
     */
    public static String holeVorhandeneTranskription(String pfad) {
        Connection conn = getConnection();
        if (conn == null) return null;

        // Um den genauen Match in der DB zu finden, kürzen wir den Pfad ggf. auf 100 Zeichen
        if (pfad.length() > 100) pfad = pfad.substring(0, 100);

        String selectSQL = "SELECT inhalt, check_status FROM tonaufnahmen WHERE pfad = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, pfad);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String inhalt = rs.getString("inhalt");
                int checkStatus = rs.getInt("check_status");
                
                // Wenn check_status NICHT 1 ist (also 0 oder NULL), überspringen wir eine Neuausführung
                if (checkStatus != 1) {
                    return inhalt;
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Prüfen auf vorhandene Transkription: " + e.getMessage());
        }

        return null;
    }

    /**
     * Aktualisiert eine bestehende Transkription oder fügt sie neu ein.
     */
    public static void speichereOderAktualisiereTranskription(String pfad, String inhalt) {
        Connection conn = getConnection();
        if (conn == null) return;

        if (pfad.length() > 100) pfad = pfad.substring(0, 100);
        if (inhalt != null && inhalt.length() > 500) inhalt = inhalt.substring(0, 500);

        // Zuerst prüfen, ob der Pfad schon existiert
        String selectSQL = "SELECT aufnahme_id FROM tonaufnahmen WHERE pfad = ?";
        boolean exists = false;
        try (PreparedStatement pstmtSelect = conn.prepareStatement(selectSQL)) {
            pstmtSelect.setString(1, pfad);
            ResultSet rs = pstmtSelect.executeQuery();
            if (rs.next()) {
                exists = true;
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Prüfen der Existenz: " + e.getMessage());
            return;
        }

        if (exists) {
            // Update
            String updateSQL = "UPDATE tonaufnahmen SET inhalt = ?, check_status = NULL WHERE pfad = ?";
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateSQL)) {
                pstmtUpdate.setString(1, inhalt);
                pstmtUpdate.setString(2, pfad);
                pstmtUpdate.executeUpdate();
                System.out.println("Transkription in Datenbank aktualisiert: " + pfad);
            } catch (SQLException e) {
                System.err.println("Fehler beim Aktualisieren in der Datenbank: " + e.getMessage());
            }
        } else {
            // Insert
            speichereTranskription(pfad, inhalt);
        }
    }
}
