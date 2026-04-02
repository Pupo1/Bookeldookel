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
                         Transkript.transkribiere(wavFile);
                         System.out.println("Verarbeitung für " + wavFile.getFileName() + " abgeschlossen.");
                     });
            } catch (IOException e) {
                System.err.println("Fehler beim Zugriff auf diyrecord: " + e.getMessage());
            }
        }
        
        // 4. Datenbankverbindung am Ende schließen
        Datenbankmanager.schliesseVerbindung();
    }
}
