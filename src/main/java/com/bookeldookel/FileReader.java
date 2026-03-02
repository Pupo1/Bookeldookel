package com.bookeldookel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileReader {
    public static String[] readCodes(String filename) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filename));
            return lines.toArray(new String[0]);
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Datei: " + e.getMessage());
            return new String[0];
        }
    }
}
