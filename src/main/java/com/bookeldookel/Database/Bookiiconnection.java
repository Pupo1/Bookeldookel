package com.bookeldookel.Database;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class Bookiiconnection {

    public static void checkAndTransfer() {
        Path resourcesDir = Paths.get("src", "main", "resources");
        Path targetDir = resourcesDir.resolve("diyrecord");
        Path mp3TargetDir = resourcesDir.resolve("mp3");

        // 0. Ordner anlegen
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            if (!Files.exists(mp3TargetDir)) {
                Files.createDirectories(mp3TargetDir);
            }
        } catch (IOException e) {
            System.err.println("Fehler bei der Vorbereitung des Zielordners: " + e.getMessage());
        }

        Path bookiiDrive = null;

        int versuche = 0;
        // Im Developer-Modus nur 1 Versuch, sonst 10
        final int MAX_VERSUCHE = Main.DEVELOPER_MODUS ? 1 : 10;

        // 1. Warten, bis der Stift (BOOKII_APP) angeschlossen ist.
        while (bookiiDrive == null && versuche < MAX_VERSUCHE) {
            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                try {
                    FileStore store = Files.getFileStore(root);
                    if ("BOOKII_APP".equalsIgnoreCase(store.name())) {
                        bookiiDrive = root;
                        break;
                    }
                } catch (IOException e) {
                    // Laufwerk nicht bereit, ignorieren
                }
            }

            if (bookiiDrive == null) {
                versuche++;
                System.out.println("Bitte den Stift mit einem USB-Kabel mit dem Computer verbinden! (Versuch " + versuche + " von " + MAX_VERSUCHE + ")");
                if (versuche >= MAX_VERSUCHE) {
                    System.out.println("Kein Stift gefunden nach " + MAX_VERSUCHE + " Versuchen. Überspringe Übertragung.");
                    return; // Beende die Methode und fahre mit dem Hauptprogramm fort
                }
                try {
                    Thread.sleep(3000); // 3 Sekunden warten
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        
        // Sollte nie erreicht werden, wenn bookiiDrive null ist und MAX_VERSUCHE erreicht wurde,
        // da wir im if-Block oben schon 'return' aufrufen. Zur Sicherheit trotzdem prüfen.
        if (bookiiDrive != null) {
            System.out.println("BOOKII_APP gefunden auf Laufwerk: " + bookiiDrive + ". Durchsuche nun alle Laufwerke nach 'diyrecord'...");

            // 2. Alle Laufwerke nach dem Ordner "diyrecord" durchsuchen.
            boolean foundAnyDiyRecordFolder = false;

            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                Path sourceDir = root.resolve("diyrecord");

                if (Files.exists(sourceDir) && Files.isDirectory(sourceDir)) {
                    foundAnyDiyRecordFolder = true;
                    System.out.println("Ordner 'diyrecord' gefunden auf Laufwerk: " + root);

                    try {
                        try (Stream<Path> paths = Files.list(sourceDir)) {
                            long count = 0;
                            for (Path source : (Iterable<Path>) paths::iterator) {
                                if (source.toString().toLowerCase().endsWith(".wav")) {
                                    try {
                                        Path target = targetDir.resolve(source.getFileName());
                                        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                                        System.out.println("  -> Verschoben: " + source.getFileName());
                                        count++;
                                    } catch (IOException e) {
                                        System.err.println("  -> Fehler beim Verschieben von " + source.getFileName() + ": " + e.getMessage());
                                    }
                                }
                            }

                            if (count == 0) {
                                System.out.println("  -> Keine .wav-Dateien in diesem Ordner gefunden.");
                            } else {
                                System.out.println("  -> " + count + " .wav-Dateien wurden erfolgreich verschoben.");
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Fehler beim Zugriff auf Verzeichnis: " + sourceDir + " - " + e.getMessage());
                    }
                }
            }

            if (!foundAnyDiyRecordFolder) {
                System.out.println("Der Ordner 'diyrecord' wurde auf keinem Laufwerk gefunden.");
            }

            // 2b. Alle Laufwerke nach dem Ordner "mp3" durchsuchen und .wav-Dateien verschieben.
            System.out.println("Durchsuche nun alle Laufwerke nach 'mp3'...");
            boolean foundAnyMp3Folder = false;

            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                Path mp3SourceDir = root.resolve("mp3");

                if (Files.exists(mp3SourceDir) && Files.isDirectory(mp3SourceDir)) {
                    foundAnyMp3Folder = true;
                    System.out.println("Ordner 'mp3' gefunden auf Laufwerk: " + root);

                    try {
                        try (Stream<Path> paths = Files.list(mp3SourceDir)) {
                            long count = 0;
                            for (Path source : (Iterable<Path>) paths::iterator) {
                                if (source.toString().toLowerCase().endsWith(".wav")) {
                                    try {
                                        Path target = mp3TargetDir.resolve(source.getFileName());
                                        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                                        System.out.println("  -> Verschoben: " + source.getFileName());
                                        count++;
                                    } catch (IOException e) {
                                        System.err.println("  -> Fehler beim Verschieben von " + source.getFileName() + ": " + e.getMessage());
                                    }
                                }
                            }

                            if (count == 0) {
                                System.out.println("  -> Keine .wav-Dateien in diesem Ordner gefunden.");
                            } else {
                                System.out.println("  -> " + count + " .wav-Dateien wurden erfolgreich nach resources/mp3 verschoben.");
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Fehler beim Zugriff auf Verzeichnis: " + mp3SourceDir + " - " + e.getMessage());
                    }
                }
            }

            if (!foundAnyMp3Folder) {
                System.out.println("Der Ordner 'mp3' wurde auf keinem Laufwerk gefunden.");
            }

            // 3. Laufwerk "BOOKII_APP" (bookiiDrive) auswerfen
            System.out.println("Versuche Laufwerk " + bookiiDrive + " auszuwerfen...");
            ejectDrive(bookiiDrive);
        }
    }

    private static void ejectDrive(Path drivePath) {
        String driveLetter = drivePath.toString().replace("\\", "").replace("/", ""); // z.B. "E:"
        
        try {
            // Dieses Skript nutzt ein COM-Objekt, um den Datenträger auszuwerfen.
            String script = "$driveEject = New-Object -comObject Shell.Application; " +
                            "$driveEject.Namespace(17).ParseName('" + driveLetter + "').InvokeVerb('Eject')";
            
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", script);
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("Laufwerk " + driveLetter + " wurde erfolgreich ausgeworfen. Du kannst den Stift nun abziehen.");
            } else {
                System.out.println("Das Auswerfen von Laufwerk " + driveLetter + " ist fehlgeschlagen (Exit Code: " + exitCode + "). Bitte wirf den Stift manuell aus.");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Fehler beim Versuch, das Laufwerk auszuwerfen: " + e.getMessage());
            System.out.println("Bitte wirf den Stift manuell aus.");
        }
    }
}
