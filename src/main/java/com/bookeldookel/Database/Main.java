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
        OrtEintragen.verarbeiteMp3Ordner();

        // 4. Alle verschobenen Dateien aus diyrecord transkribieren und in die DB speichern
        Transkript.transkribiereVerzeichnis();
        
        // 5. Transkriptionen prüfen
        TranskriptionsPruefer.pruefeTranskriptionen();
        
        // 6. KI-gestützte Artikelzuordnung starten
        WohinDamit.ermittleZiel();
        
        // 7. Orte genehmigen
        OrteGenehmigen.pruefeOrte();

        // 8. Audios kombinieren und auf den Stift übertragen (Neu: ohne Text-to-Speech)
        AudioGenerator.generiereUndUebertrageAudios();
        
        // 9. Datenbankverbindung am Ende schließen
        Datenbankmanager.schliesseVerbindung();
    }
}
