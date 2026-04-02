package com.bookeldookel.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
}
