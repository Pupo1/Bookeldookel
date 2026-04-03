package Testklassen;

import com.bookeldookel.Database.Datenbankmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasse zum Auslesen und Präsentieren der Schachteln mit ihrem Inhalt.
 * Wenn kein Inhalt in der Schachtel gespeichert ist, werden die Bezeichnungen
 * der zugehörigen Artikel aus der Artikel-Tabelle ausgelesen.
 */
public class SchachtelInhaltAnzeiger {

    public static void main(String[] args) {
        zeigeSchachtelInhalte();
        Datenbankmanager.schliesseVerbindung();
    }

    /**
     * Liest alle Schachteln aus und zeigt deren Inhalt an.
     * Falls kein Inhalt vorhanden ist, werden die Bezeichnungen der
     * zugehörigen Artikel angezeigt.
     */
    public static void zeigeSchachtelInhalte() {
        Connection conn = Datenbankmanager.getConnection();
        if (conn == null) {
            System.err.println("Keine Datenbankverbindung möglich.");
            return;
        }

        String selectSchachteln = "SELECT schachtel_id, Inhalt FROM schachtel";

        try (PreparedStatement stmt = conn.prepareStatement(selectSchachteln);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int schachtelId = rs.getInt("schachtel_id");
                String inhalt = rs.getString("Inhalt");

                System.out.println("=================================");
                System.out.println("Schachtel ID: " + schachtelId);

                if (inhalt != null && !inhalt.trim().isEmpty()) {
                    System.out.println("Inhalt: " + inhalt);
                } else {
                    System.out.println("Inhalt: (leer - Artikel werden geladen)");
                    List<String> artikelBezeichnungen = holeArtikelBezeichnungen(conn, schachtelId);
                    if (artikelBezeichnungen.isEmpty()) {
                        System.out.println("  Keine Artikel gefunden.");
                    } else {
                        for (String bezeichnung : artikelBezeichnungen) {
                            System.out.println("  - " + bezeichnung);
                        }
                    }
                }
            }
            System.out.println("=================================");

        } catch (SQLException e) {
            System.err.println("Fehler beim Auslesen der Schachteln: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Holt alle Artikelbezeichnungen für eine bestimmte Schachtel-ID.
     *
     * @param conn        Die Datenbankverbindung
     * @param schachtelId Die ID der Schachtel
     * @return Liste der Artikelbezeichnungen
     */
    private static List<String> holeArtikelBezeichnungen(Connection conn, int schachtelId) {
        List<String> bezeichnungen = new ArrayList<>();
        String selectArtikel = "SELECT Bezeichnung FROM artikel WHERE schachtel_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(selectArtikel)) {
            stmt.setInt(1, schachtelId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String bezeichnung = rs.getString("Bezeichnung");
                    if (bezeichnung != null) {
                        bezeichnungen.add(bezeichnung);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Auslesen der Artikel für Schachtel " + schachtelId + ": " + e.getMessage());
        }

        return bezeichnungen;
    }
}
