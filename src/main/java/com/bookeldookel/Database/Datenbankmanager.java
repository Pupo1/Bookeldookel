package com.bookeldookel.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

        // Tabelle artikel anlegen, falls noch nicht vorhanden
        String createArtikelTableSQL = "CREATE TABLE IF NOT EXISTS artikel ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "schachtel_id INT DEFAULT NULL, "
                + "bezeichnung VARCHAR(255)"
                + ");";

        // Tabelle schachtel anlegen, falls noch nicht vorhanden
        String createSchachtelTableSQL = "CREATE TABLE IF NOT EXISTS schachtel ("
                + "schachtel_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "NummerAufDerSchachtel INT NOT NULL DEFAULT 0, "
                + "glied_id INT DEFAULT NULL, "
                + "Tondateipfad VARCHAR(200) DEFAULT NULL, "
                + "sort_order INT DEFAULT NULL, "
                + "Inhalt VARCHAR(255) DEFAULT NULL, "
                + "Ort VARCHAR(400) DEFAULT NULL"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createArtikelTableSQL);
            stmt.execute(createSchachtelTableSQL);
            System.out.println("Tabellen 'tonaufnahmen', 'artikel' und 'schachtel' geprüft/erstellt.");
            
            // MetaData verwenden, um Spalten zu prüfen (vermeidet SQL-Warnungen in der Konsole)
            java.sql.DatabaseMetaData dbm = conn.getMetaData();
            
            // Prüfen auf Spalte 'zuordnung' in tonaufnahmen
            try (ResultSet rs = dbm.getColumns(null, null, "tonaufnahmen", "zuordnung")) {
                if (!rs.next()) {
                    System.out.println("Spalte 'zuordnung' fehlt in 'tonaufnahmen', wird hinzugefügt...");
                    stmt.execute("ALTER TABLE tonaufnahmen ADD COLUMN zuordnung VARCHAR(255)");
                    System.out.println("Spalte 'zuordnung' erfolgreich hinzugefügt.");
                }
            }

            // Prüfen auf Spalte 'zuordnung_check' in tonaufnahmen
            try (ResultSet rs = dbm.getColumns(null, null, "tonaufnahmen", "zuordnung_check")) {
                if (!rs.next()) {
                    System.out.println("Spalte 'zuordnung_check' fehlt in 'tonaufnahmen', wird hinzugefügt...");
                    stmt.execute("ALTER TABLE tonaufnahmen ADD COLUMN zuordnung_check TINYINT(1) DEFAULT NULL");
                    System.out.println("Spalte 'zuordnung_check' erfolgreich hinzugefügt.");
                }
            }
            
            // Prüfen auf Spalte 'schachtel_id' in tonaufnahmen
            try (ResultSet rs = dbm.getColumns(null, null, "tonaufnahmen", "schachtel_id")) {
                if (!rs.next()) {
                    System.out.println("Spalte 'schachtel_id' fehlt in 'tonaufnahmen', wird hinzugefügt...");
                    stmt.execute("ALTER TABLE tonaufnahmen ADD COLUMN schachtel_id INT DEFAULT NULL");
                    System.out.println("Spalte 'schachtel_id' erfolgreich hinzugefügt.");
                }
            }

            // Prüfen auf Spalte 'Ort' in 'schachtel' (nur falls noch nicht durch CREATE TABLE oben abgedeckt in Altsystemen)
            try (ResultSet rs = dbm.getColumns(null, null, "schachtel", "Ort")) {
                if (!rs.next()) {
                    System.out.println("Spalte 'Ort' fehlt in 'schachtel', wird hinzugefügt...");
                    stmt.execute("ALTER TABLE schachtel ADD COLUMN Ort VARCHAR(400) DEFAULT NULL");
                    System.out.println("Spalte 'Ort' erfolgreich hinzugefügt.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Erstellen der Tabelle/Spalte: " + e.getMessage());
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
            System.out.println("Transkription in Datenbank gespeichert: " + pfad + " -> " + inhalt);
        } catch (SQLException e) {
            System.err.println("Fehler beim Speichern in der Datenbank: " + e.getMessage());
        }
    }

    public static String holeVorhandeneTranskription(String pfad) {
        Connection conn = getConnection();
        if (conn == null) return null;

        if (pfad.length() > 100) pfad = pfad.substring(0, 100);

        String selectSQL = "SELECT inhalt, check_status FROM tonaufnahmen WHERE pfad = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, pfad);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String inhalt = rs.getString("inhalt");
                int checkStatus = rs.getInt("check_status");
                
                if (checkStatus != 1) {
                    return inhalt;
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Prüfen auf vorhandene Transkription: " + e.getMessage());
        }

        return null;
    }

    public static void speichereOderAktualisiereTranskription(String pfad, String inhalt) {
        Connection conn = getConnection();
        if (conn == null) return;

        if (pfad.length() > 100) pfad = pfad.substring(0, 100);
        if (inhalt != null && inhalt.length() > 500) inhalt = inhalt.substring(0, 500);

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
            String updateSQL = "UPDATE tonaufnahmen SET inhalt = ?, check_status = NULL WHERE pfad = ?";
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateSQL)) {
                pstmtUpdate.setString(1, inhalt);
                pstmtUpdate.setString(2, pfad);
                pstmtUpdate.executeUpdate();
                System.out.println("Transkription in Datenbank aktualisiert: " + pfad + " -> " + inhalt);
            } catch (SQLException e) {
                System.err.println("Fehler beim Aktualisieren in der Datenbank: " + e.getMessage());
            }
        } else {
            speichereTranskription(pfad, inhalt);
        }
    }

    public static List<String> holeAlleArtikel() {
        Connection conn = getConnection();
        List<String> artikelListe = new ArrayList<>();
        if (conn == null) return artikelListe;

        String selectSQL = "SELECT bezeichnung FROM artikel";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                artikelListe.add(rs.getString("bezeichnung"));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Artikel: " + e.getMessage());
        }
        return artikelListe;
    }

    public static List<String> holeAlleSchachtelInhalte() {
        Connection conn = getConnection();
        List<String> inhaltListe = new ArrayList<>();
        if (conn == null) return inhaltListe;

        String selectSQL = "SELECT Inhalt FROM schachtel WHERE Inhalt IS NOT NULL AND Inhalt != ''";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                inhaltListe.add(rs.getString("Inhalt"));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Schachtel-Inhalte: " + e.getMessage());
        }
        return inhaltListe;
    }

    public static String holeSchachtelInhaltZuArtikel(String artikelBezeichnung) {
        Connection conn = getConnection();
        if (conn == null) return null;

        String selectSQL = "SELECT s.Inhalt FROM artikel a JOIN schachtel s ON a.schachtel_id = s.schachtel_id WHERE a.bezeichnung = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, artikelBezeichnung);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("Inhalt");
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden des Schachtel-Inhalts für Artikel '" + artikelBezeichnung + "': " + e.getMessage());
        }

        return null; // Kein passender Inhalt gefunden
    }

    public static Integer getSchachtelIdByInhalt(String inhalt) {
        Connection conn = getConnection();
        if (conn == null) return null;

        String selectSQL = "SELECT schachtel_id FROM schachtel WHERE Inhalt = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, inhalt);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("schachtel_id");
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der schachtel_id über den Inhalt: " + e.getMessage());
        }
        return null;
    }

    public static Integer getSchachtelIdByArtikelBezeichnung(String bezeichnung) {
        Connection conn = getConnection();
        if (conn == null) return null;

        String selectSQL = "SELECT schachtel_id FROM artikel WHERE bezeichnung = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, bezeichnung);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int schachtelId = rs.getInt("schachtel_id");
                // Da in der DB NULL als 0 zurückgegeben werden kann von getInt(), prüfen wir auf wasNull()
                if (rs.wasNull()) {
                    return null;
                }
                return schachtelId;
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der schachtel_id über die Artikelbezeichnung: " + e.getMessage());
        }
        return null;
    }

    public static Tonaufnahme getErsteGepruefteTranskription() {
        Connection conn = getConnection();
        if (conn == null) return null;

        String selectSQL = "SELECT aufnahme_id, pfad, inhalt FROM tonaufnahmen WHERE check_status = 1 AND uebertragen IS NULL LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            if (rs.next()) {
                return new Tonaufnahme(rs.getInt("aufnahme_id"), rs.getString("pfad"), rs.getString("inhalt"));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der ersten geprüften Transkription: " + e.getMessage());
        }
        return null;
    }

    public static List<Tonaufnahme> holeAlleGeprueftenTranskriptionen() {
        Connection conn = getConnection();
        List<Tonaufnahme> list = new ArrayList<>();
        if (conn == null) return list;

        String selectSQL = "SELECT aufnahme_id, pfad, inhalt FROM tonaufnahmen WHERE check_status = 1 AND uebertragen IS NULL";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                list.add(new Tonaufnahme(rs.getInt("aufnahme_id"), rs.getString("pfad"), rs.getString("inhalt")));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden aller geprüften Transkriptionen: " + e.getMessage());
        }
        return list;
    }

    public static void speichereZuordnung(int aufnahmeId, String zuordnung, Integer schachtelId) {
        Connection conn = getConnection();
        if (conn == null) return;

        if (zuordnung != null && zuordnung.length() > 255) {
            zuordnung = zuordnung.substring(0, 255);
        }

        String updateSQL = "UPDATE tonaufnahmen SET zuordnung = ?, schachtel_id = ?, uebertragen = 1 WHERE aufnahme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setString(1, zuordnung);
            if (schachtelId != null) {
                pstmt.setInt(2, schachtelId);
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            }
            pstmt.setInt(3, aufnahmeId);
            pstmt.executeUpdate();
            System.out.println("Zuordnung '" + zuordnung + "' (Schachtel-ID: " + schachtelId + ") erfolgreich gespeichert.");
        } catch (SQLException e) {
            System.err.println("Fehler beim Speichern der Zuordnung in Datenbank: " + e.getMessage());
        }
    }

    public static String getOrtFuerSchachtel(int nummerAufSchachtel) {
        Connection conn = getConnection();
        if (conn == null) return "NOT_FOUND";

        String selectSQL = "SELECT Ort FROM schachtel WHERE NummerAufDerSchachtel = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setInt(1, nummerAufSchachtel);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("Ort");
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abfragen der Schachtel: " + e.getMessage());
        }
        return "NOT_FOUND";
    }

    public static void neueSchachtelAnlegen(int nummerAufSchachtel, String ort, String tondateipfad) {
        Connection conn = getConnection();
        if (conn == null) return;

        if (ort != null && ort.length() > 400) ort = ort.substring(0, 400);
        if (tondateipfad != null && tondateipfad.length() > 200) tondateipfad = tondateipfad.substring(0, 200);

        String insertSQL = "INSERT INTO schachtel (NummerAufDerSchachtel, Ort, Tondateipfad) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setInt(1, nummerAufSchachtel);
            pstmt.setString(2, ort);
            pstmt.setString(3, tondateipfad);
            pstmt.executeUpdate();
            System.out.println("Neue Schachtel " + nummerAufSchachtel + " angelegt. Ort: " + ort);
        } catch (SQLException e) {
            System.err.println("Fehler beim Anlegen der neuen Schachtel: " + e.getMessage());
        }
    }
    
    public static void updateSchachtelOrt(int nummerAufSchachtel, String neuerOrt, String tondateipfad) {
        Connection conn = getConnection();
        if (conn == null) return;

        if (neuerOrt != null && neuerOrt.length() > 400) neuerOrt = neuerOrt.substring(0, 400);
        if (tondateipfad != null && tondateipfad.length() > 200) tondateipfad = tondateipfad.substring(0, 200);

        String updateSQL = "UPDATE schachtel SET Ort = ?, Tondateipfad = ? WHERE NummerAufDerSchachtel = ?";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateSQL)) {
            pstmtUpdate.setString(1, neuerOrt);
            pstmtUpdate.setString(2, tondateipfad);
            pstmtUpdate.setInt(3, nummerAufSchachtel);
            pstmtUpdate.executeUpdate();
            System.out.println("Ort für Schachtel " + nummerAufSchachtel + " aktualisiert auf: " + neuerOrt);
        } catch (SQLException e) {
            System.err.println("Fehler beim Aktualisieren des Schachtel-Ortes: " + e.getMessage());
        }
    }

    public static void inhaltZuSchachtelHinzufuegen(int nummerAufSchachtel, String neuerInhalt) {
        Connection conn = getConnection();
        if (conn == null) return;

        String selectSQL = "SELECT Inhalt FROM schachtel WHERE NummerAufDerSchachtel = ?";
        String bisherigerInhalt = null;
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setInt(1, nummerAufSchachtel);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                bisherigerInhalt = rs.getString("Inhalt");
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen des Schachtel-Inhalts: " + e.getMessage());
            return;
        }

        String kombinierterInhalt = (bisherigerInhalt != null && !bisherigerInhalt.trim().isEmpty()) 
                                    ? bisherigerInhalt + " | " + neuerInhalt 
                                    : neuerInhalt;
        
        if (kombinierterInhalt.length() > 255) {
            kombinierterInhalt = kombinierterInhalt.substring(0, 255);
        }

        String updateSQL = "UPDATE schachtel SET Inhalt = ? WHERE NummerAufDerSchachtel = ?";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateSQL)) {
            pstmtUpdate.setString(1, kombinierterInhalt);
            pstmtUpdate.setInt(2, nummerAufSchachtel);
            pstmtUpdate.executeUpdate();
            System.out.println("Inhalt zur Schachtel " + nummerAufSchachtel + " hinzugefügt.");
        } catch (SQLException e) {
            System.err.println("Fehler beim Aktualisieren des Schachtel-Inhalts: " + e.getMessage());
        }
    }

    public static class Tonaufnahme {
        public int id;
        public String pfad;
        public String inhalt;

        public Tonaufnahme(int id, String pfad, String inhalt) {
            this.id = id;
            this.pfad = pfad;
            this.inhalt = inhalt;
        }
    }
}
