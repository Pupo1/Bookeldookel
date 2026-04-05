package com.bookeldookel.Database;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrtEintragen {

    // Für Audio-Transkription verwenden wir Gemini 2.5 Flash
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final BufferedReader KONSOLE = new BufferedReader(new InputStreamReader(System.in));

    public static void verarbeiteMp3Ordner() {
        System.out.println("\n=== Starte Verarbeitung von .wav-Dateien aus dem mp3-Ordner (OrtEintragen) ===");

        Path mp3Dir = Paths.get("src", "main", "resources", "mp3");
        Path archivDir = Paths.get("src", "main", "resources", "archiv");

        if (!Files.exists(mp3Dir) || !Files.isDirectory(mp3Dir)) {
            System.out.println("Der Ordner 'mp3' existiert nicht. Überspringe Ortseintrag.");
            return;
        }

        try {
            if (!Files.exists(archivDir)) {
                Files.createDirectories(archivDir);
            }
        } catch (IOException e) {
            System.err.println("Konnte Archiv-Ordner nicht erstellen: " + e.getMessage());
            return;
        }

        // Initialisiere die Datenbankverbindung und -struktur, falls noch nicht geschehen
        Datenbankmanager.erstelleTabelleFallsNichtExistent();

        try (Stream<Path> paths = Files.list(mp3Dir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".wav") && !Files.isDirectory(p))
                 .forEach(wavFile -> {
                     System.out.println("\nVerarbeite Datei: " + wavFile.getFileName());

                     // 1. Transkribieren
                     String transkription = transkribiere(wavFile);

                     if (transkription == null) {
                         System.out.println("Transkription fehlgeschlagen (Ergebnis ist null). Datei wird übersprungen.");
                         return; // Continue mit nächster Datei
                     } else if (isErrorText(transkription)) {
                         System.out.println("Transkription ungültig, da sie als Fehlertext erkannt wurde.");
                         System.out.println("Erkannter Text war: " + transkription);
                         return;
                     }

                     System.out.println("Erkannter Text: " + transkription);

                     // 2. Prüfen, ob "Inhalt von <Zahl>" oder nur eine Zahl am Anfang steht
                     Pattern inhaltPattern = Pattern.compile("(?i)^\\D*inhalt\\s+von\\s+(\\d+)");
                     Matcher inhaltMatcher = inhaltPattern.matcher(transkription);
                     
                     Pattern zahlPattern = Pattern.compile("^\\D*(\\d+)");
                     Matcher zahlMatcher = zahlPattern.matcher(transkription);

                     if (inhaltMatcher.find()) {
                         // Es handelt sich um eine Inhaltsangabe
                         String zahlStr = inhaltMatcher.group(1);
                         int nummer = Integer.parseInt(zahlStr);
                         int endeIndex = inhaltMatcher.end(1);
                         String restText = transkription.substring(endeIndex).trim();
                         
                         System.out.println("Inhaltsangabe für Schachtel " + nummer + " erkannt.");
                         System.out.println("Neuer Inhalt: " + restText);
                         
                         verarbeiteDatenbankInhalt(nummer, restText, wavFile, archivDir);

                     } else if (zahlMatcher.find()) {
                         // Es handelt sich um eine Ort-Registrierung (nur Zahl am Anfang)
                         String zahlStr = zahlMatcher.group(1);
                         int nummer = Integer.parseInt(zahlStr);
                         int endeIndex = zahlMatcher.end(1);
                         String restText = transkription.substring(endeIndex).trim();

                         System.out.println("Ort-Registrierung für Schachtel " + nummer + " erkannt.");
                         System.out.println("Ort: " + restText);

                         // Sicherheitsabfrage mit Korrekturmöglichkeit
                         int[] bestaetigteWerte = frageBenutzerNachBestaetigung(nummer, restText);
                         
                         if (bestaetigteWerte != null) {
                             int endgueltigeNummer = bestaetigteWerte[0];
                             String endgueltigerOrt = bestaetigteWerte[1] == -1 ? restText : 
                                     holeKorrigiertenOrt();
                             
                             verarbeiteDatenbankOrt(endgueltigeNummer, endgueltigerOrt, wavFile, archivDir);
                         } else {
                             System.out.println("Eintrag wurde vom Benutzer abgebrochen.");
                         }

                     } else {
                         System.err.println("FEHLER: Keine Zahl am Anfang des Textes gefunden. Datei wird übersprungen.");
                     }
                     
                     // Kurze Pause, um Rate-Limits bei vielen Dateien zu vermeiden
                     try {
                         Thread.sleep(8000);
                     } catch (InterruptedException e) {
                         Thread.currentThread().interrupt();
                     }
                 });

        } catch (IOException e) {
            System.err.println("Fehler beim Zugriff auf mp3-Ordner: " + e.getMessage());
        }
        
        System.out.println("\n=== Verarbeitung von .wav-Dateien (OrtEintragen) beendet ===");
    }

    // Speichert den korrigierten Ort zwischen
    private static String letzterKorrigierterOrt = null;

    /**
     * Fragt den Benutzer nach Bestätigung oder Korrektur der erkannten Daten.
     * 
     * @param vorgeschlageneNummer Die aus der Transkription erkannte Schachtelnummer
     * @param vorgeschlagenerOrt Der aus der Transkription erkannte Ort
     * @return Array mit [nummer, flag] wobei flag=-1 bedeutet "Ort unverändert", oder null bei Abbruch
     */
    private static int[] frageBenutzerNachBestaetigung(int vorgeschlageneNummer, String vorgeschlagenerOrt) {
        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println("                    SICHERHEITSABFRAGE                          ");
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println("  Schachtel-Nr: " + vorgeschlageneNummer);
        System.out.println("  Ort: " + vorgeschlagenerOrt);
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("  [Enter] = Bestaetigen");
        System.out.println("  Korrektur eingeben: \"[Nummer] [Bezeichnung]\"");
        System.out.println("  \"abbruch\" = Vorgang abbrechen");
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.print("Ihre Eingabe: ");

        try {
            String eingabe = KONSOLE.readLine();
            
            if (eingabe == null) {
                return null;
            }
            
            eingabe = eingabe.trim();
            
            // Leere Eingabe = Bestätigung des Vorschlags
            if (eingabe.isEmpty()) {
                System.out.println("Vorschlag bestaetigt.");
                letzterKorrigierterOrt = vorgeschlagenerOrt;
                return new int[]{vorgeschlageneNummer, -1};
            }
            
            // Abbruch
            if (eingabe.equalsIgnoreCase("abbruch") || eingabe.equalsIgnoreCase("abbrechen")) {
                return null;
            }
            
            // Korrektur parsen: "1012 Sicherungskasten" -> Nummer=1012, Ort="Sicherungskasten"
            Pattern korrekturPattern = Pattern.compile("^(\\d+)\\s+(.+)$");
            Matcher korrekturMatcher = korrekturPattern.matcher(eingabe);
            
            if (korrekturMatcher.find()) {
                int korrigierteNummer = Integer.parseInt(korrekturMatcher.group(1));
                String korrigierterOrt = korrekturMatcher.group(2).trim();
                
                System.out.println("Korrektur uebernommen: Schachtel " + korrigierteNummer + ", Ort: " + korrigierterOrt);
                letzterKorrigierterOrt = korrigierterOrt;
                return new int[]{korrigierteNummer, 1};
            } else {
                System.err.println("Ungueltiges Eingabeformat. Erwartet: \"<Nummer> <Ort>\" (z.B. \"1012 Sicherungskasten\")");
                System.out.println("Verwende urspruenglichen Vorschlag.");
                letzterKorrigierterOrt = vorgeschlagenerOrt;
                return new int[]{vorgeschlageneNummer, -1};
            }
            
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Eingabe: " + e.getMessage());
            return null;
        }
    }

    /**
     * Holt den korrigierten Ort aus dem letzten Durchlauf.
     */
    private static String holeKorrigiertenOrt() {
        return letzterKorrigierterOrt;
    }

    private static void verarbeiteDatenbankOrt(int nummer, String ort, Path originalDatei, Path archivDir) {
        String existierenderOrt = Datenbankmanager.getOrtFuerSchachtel(nummer);

        try {
            Path zielPfad = findeEindeutigenZielpfad(archivDir, originalDatei.getFileName().toString());
            String tondateipfad = zielPfad.toAbsolutePath().toString().replace("\\", "/");

            if ("NOT_FOUND".equals(existierenderOrt)) {
                // Schachtel existiert noch nicht -> Neu anlegen mit Ort
                Datenbankmanager.neueSchachtelAnlegen(nummer, ort, tondateipfad);
            } else {
                // Schachtel existiert. Neue Anforderung: Immer überschreiben/aktualisieren.
                System.out.println("Schachtel " + nummer + " existiert bereits. Ort wird ueberschrieben/aktualisiert.");
                Datenbankmanager.updateSchachtelOrt(nummer, ort, tondateipfad);
            }
            
            // Datei ins Archiv verschieben
            Files.move(originalDatei, zielPfad, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Datei (Ort-Registrierung) archiviert nach: " + zielPfad);
            
        } catch (IOException e) {
            System.err.println("Fehler beim Verschieben der Datei ins Archiv: " + e.getMessage());
        }
    }

    private static void verarbeiteDatenbankInhalt(int nummer, String neuerInhalt, Path originalDatei, Path archivDir) {
        String existierenderOrt = Datenbankmanager.getOrtFuerSchachtel(nummer);
        
        if ("NOT_FOUND".equals(existierenderOrt)) {
            // Die Schachtel existiert noch gar nicht. Wir können sie anlegen, Ort bleibt null, Inhalt wird befüllt.
            System.out.println("Schachtel " + nummer + " existiert noch nicht. Wird angelegt.");
            Datenbankmanager.neueSchachtelAnlegen(nummer, null, null);
        }
        
        // Inhalt hinzufügen
        Datenbankmanager.inhaltZuSchachtelHinzufuegen(nummer, neuerInhalt);
        
        try {
            // Da es sich um Inhalt handelte, wird der Dateiname NICHT in die DB eingetragen.
            // Die Datei wird aber laut Anforderung trotzdem ins Archiv verschoben.
            Path zielPfad = findeEindeutigenZielpfad(archivDir, originalDatei.getFileName().toString());
            Files.move(originalDatei, zielPfad, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Datei (Inhalt) archiviert nach: " + zielPfad);
        } catch (IOException e) {
            System.err.println("Fehler beim Verschieben der Datei ins Archiv: " + e.getMessage());
        }
    }

    private static Path findeEindeutigenZielpfad(Path archivDir, String dateiName) {
        Path zielPfad = archivDir.resolve(dateiName);
        if (!Files.exists(zielPfad)) {
            return zielPfad;
        }

        String basisName = dateiName;
        String erweiterung = "";
        int letzterPunkt = dateiName.lastIndexOf('.');
        if (letzterPunkt > 0) {
            basisName = dateiName.substring(0, letzterPunkt);
            erweiterung = dateiName.substring(letzterPunkt);
        }

        int zaehler = 1;
        while (Files.exists(zielPfad)) {
            String neuerName = basisName + "_" + zaehler + erweiterung;
            zielPfad = archivDir.resolve(neuerName);
            zaehler++;
        }
        return zielPfad;
    }

    private static String transkribiere(Path audioFile) {
        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("FEHLER: API Key fehlt.");
            return null;
        }

        try (Client client = Client.builder().apiKey(apiKey).build()) {
            byte[] audioBytes = Files.readAllBytes(audioFile);
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            Part audioPart = Part.builder()
                    .inlineData(com.google.genai.types.Blob.builder()
                            .mimeType("audio/wav")
                            .data(base64Audio)
                            .build())
                    .build();

            Part textPart = Part.builder()
                    .text("Bitte transkribiere diese Audioaufnahme exakt. Schreibe alle Zahlen als Ziffern (1016, 50, etc.). Keine Kommentare.")
                    .build();

            Content content = Content.builder()
                    .parts(Arrays.asList(audioPart, textPart))
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                MODEL_NAME, 
                content, 
                GenerateContentConfig.builder().build()
            );
            
            return response.text() != null ? response.text().trim() : null;

        } catch (Exception e) {
            System.err.println("Fehler bei der KI-Anfrage: " + e.getMessage());
            return null;
        }
    }

    private static boolean isErrorText(String text) {
        if (text == null) return true;
        String lowerText = text.toLowerCase();
        return lowerText.contains("error") || 
               lowerText.contains("fehler") || 
               lowerText.contains("429 too many requests") ||
               lowerText.contains("quota exceeded") ||
               lowerText.contains("rate limit");
    }
}
