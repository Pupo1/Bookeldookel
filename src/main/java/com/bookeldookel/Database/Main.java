package com.bookeldookel.Database;

public class Main {
    // Globale Variable für den Developer-Modus
    public static final boolean DEVELOPER_MODUS = true;
    
    // Globale Variable, um zu tracken, ob Fehler aufgetreten sind (z.B. API Quota-Limits)
    public static boolean fehlerAufgetreten = false;

    public static void main(String[] args) {
        // 1. Datenbank initialisieren (Tabelle erstellen, falls nicht vorhanden)
        Transkript.initDatabase();

        // 2. Bookii-Stift verbinden und Dateien übertragen
        Bookiiconnection.checkAndTransfer();

        // 3. Neue Logik: mp3-Ordner für Schachteln verarbeiten
        // Setze fehlerAufgetreten ggf. auch hier, falls die Logik Fehler wirft. 
        // Aktuell ignoriert die mp3-Ordner-Methode das globale Flag, aber wir lassen es so.
        OrtEintragen.verarbeiteMp3Ordner();

        // 4. Alle verschobenen Dateien aus diyrecord transkribieren und in die DB speichern
        // Dies setzt fehlerAufgetreten auf true, falls die API streikt.
        Transkript.transkribiereVerzeichnis();
        
        // 5. Transkriptionen prüfen
        TranskriptionsPruefer.pruefeTranskriptionen();
        
        // 6. KI-gestützte Artikelzuordnung starten
        WohinDamit.ermittleZiel();
        
        // 7. Orte genehmigen
        OrteGenehmigen.pruefeOrte();
        
        // 8. Datenbankverbindung am Ende schließen
        Datenbankmanager.schliesseVerbindung();
    }
}
