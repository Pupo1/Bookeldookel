package com.bookeldookel.Database;

public class Main {
    // Globale Variable für den Developer-Modus
    public static final boolean DEVELOPER_MODUS = true;

    public static void main(String[] args) {
        // 1. Datenbank initialisieren (Tabelle erstellen, falls nicht vorhanden)
        Transkript.initDatabase();

        // 2. Bookii-Stift verbinden und Dateien übertragen
        Bookiiconnection.checkAndTransfer();

        // 3. Alle verschobenen Dateien transkribieren und in die DB speichern
        Transkript.transkribiereVerzeichnis();
        
        // 4. Transkriptionen prüfen
        TranskriptionsPruefer.pruefeTranskriptionen();
        
        // 5. KI-gestützte Artikelzuordnung starten
        WohinDamit.ermittleZiel();
        
        // 6. Datenbankverbindung am Ende schließen
        Datenbankmanager.schliesseVerbindung();
    }
}
