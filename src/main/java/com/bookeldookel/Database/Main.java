package com.bookeldookel.Database;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Bookiiconnection.checkAndTransfer();

        // Transkribiere alle verschobenen Dateien
        Path diyrecordDir = Paths.get("src", "main", "resources", "diyrecord");

        if (Files.exists(diyrecordDir) && Files.isDirectory(diyrecordDir)) {
            try (Stream<Path> paths = Files.list(diyrecordDir)) {
                paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                     .forEach(wavFile -> {
                         String transkript = Transkript.transkribiere(wavFile);
                         System.out.println("Transkription für " + wavFile.getFileName() + " abgeschlossen.");
                     });
            } catch (IOException e) {
                System.err.println("Fehler beim Zugriff auf diyrecord: " + e.getMessage());
            }
        }
    }
}
