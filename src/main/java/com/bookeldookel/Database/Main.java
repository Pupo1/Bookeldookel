package com.bookeldookel.Database;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        // 1. Datenbank initialisieren (Tabelle erstellen, falls nicht vorhanden)
        Transkript.initDatabase();

        // 2. Bookii-Stift verbinden und Dateien übertragen
        Bookiiconnection.checkAndTransfer();

        // 3. Alle verschobenen Dateien transkribieren und in die DB speichern
        Path diyrecordDir = Paths.get("src", "main", "resources", "diyrecord");

        if (Files.exists(diyrecordDir) && Files.isDirectory(diyrecordDir)) {
            try (Stream<Path> paths = Files.list(diyrecordDir)) {
                paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                     .forEach(wavFile -> {
                         // Hole den Pfad als String wie er in der DB stehen würde
                         String pfadStr = wavFile.toAbsolutePath().toString();
                         if (pfadStr.length() > 100) pfadStr = pfadStr.substring(0, 100);

                         String vorhandenerInhalt = Datenbankmanager.holeVorhandeneTranskription(pfadStr);
                         
                         if (vorhandenerInhalt != null) {
                             // Datei existiert schon und check != 1 -> wird nicht neu transkribiert
                             System.out.println("Datei " + wavFile.getFileName() + " wurde bereits transkribiert. Überspringe API-Aufruf.");
                             System.out.println("  -> Vorhandener Text in DB: " + vorhandenerInhalt);
                         } else {
                             // Muss (neu) transkribiert werden
                             Transkript.transkribiere(wavFile);
                             System.out.println("Verarbeitung für " + wavFile.getFileName() + " abgeschlossen.");
                             try {
                                 // Warte 16 Sekunden (13 Sekunden + 3 Sekunden extra) für mehr Puffer
                                 Thread.sleep(16000);
                             } catch (InterruptedException e) {
                                 Thread.currentThread().interrupt();
                             }
                         }
                     });
            } catch (IOException e) {
                System.err.println("Fehler beim Zugriff auf diyrecord: " + e.getMessage());
            }
        }
        
        // 4. Datenbankverbindung am Ende schließen
        Datenbankmanager.schliesseVerbindung();
    }
}
