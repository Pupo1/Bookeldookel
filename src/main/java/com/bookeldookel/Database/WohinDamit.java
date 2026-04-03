package com.bookeldookel.Database;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentConfig;

import java.util.List;
import java.util.Arrays;

public class WohinDamit {
    
    private static final String MODEL_NAME = "gemma-3-12b-it";

    public static void ermittleZiel() {
        System.out.println("\n=== Starte KI-gestützte Artikelzuordnung (WohinDamit) ===");

        // 1. Alle passenden Einträge suchen (check_status = 1 und noch nicht zugeordnet)
        List<Datenbankmanager.Tonaufnahme> aufnahmen = Datenbankmanager.holeAlleGeprueftenTranskriptionen();
        
        if (aufnahmen.isEmpty()) {
            System.out.println("Es wurden keine geprüften Transkriptionen (check_status = true) gefunden, die noch nicht zugeordnet wurden.");
            return;
        }

        System.out.println(aufnahmen.size() + " geprüfte Transkription(en) gefunden. Starte Verarbeitung...");

        // 2. Artikel aus der Datenbank laden (müssen wir nur einmal machen)
        List<String> artikelListe = Datenbankmanager.holeAlleArtikel();
        if (artikelListe.isEmpty()) {
            System.out.println("Die Tabelle 'artikel' ist leer. Eine Zuordnung ist nicht möglich.");
            return;
        }

        // 3. API Key prüfen
        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("FEHLER: Die Umgebungsvariable 'BOOKELDOOKEL_API_KEY' ist nicht gesetzt.");
            return;
        }

        // Wir erstellen den Client einmal für alle Anfragen
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            
            // Vorbereiten des festen Teils des Prompts
            StringBuilder basisPromptBuilder = new StringBuilder();
            basisPromptBuilder.append("Du bist ein intelligenter Lager-Assistent.\n");
            basisPromptBuilder.append("Hier ist eine Liste aller Artikel, die bereits in meinem System existieren:\n");
            for (String artikel : artikelListe) {
                basisPromptBuilder.append("- ").append(artikel).append("\n");
            }
            basisPromptBuilder.append("\nAufgabe: Zu welchem Artikel aus der Liste passt der folgende transkribierte Text am besten?\n");
            basisPromptBuilder.append("Bitte antworte NUR mit dem exakten Namen des passenden Artikels aus der Liste. Wenn gar kein Artikel aus der Liste passt, antworte mit 'NEUER_ARTIKEL'.\n\n");
            basisPromptBuilder.append("Transkribierter Text:\n");

            String basisPrompt = basisPromptBuilder.toString();

            // Schleife über alle Aufnahmen
            for (int i = 0; i < aufnahmen.size(); i++) {
                Datenbankmanager.Tonaufnahme aufnahme = aufnahmen.get(i);
                System.out.println("\nVerarbeite Aufnahme ID " + aufnahme.id + ": \"" + aufnahme.inhalt + "\"");

                // Individuellen Prompt zusammensetzen
                String aktuellerPrompt = basisPrompt + "\"" + aufnahme.inhalt + "\"";

                Part textPart = Part.builder().text(aktuellerPrompt).build();
                Content content = Content.builder().parts(Arrays.asList(textPart)).build();

                System.out.print("Sende Anfrage an Gemini (" + MODEL_NAME + ") ... ");

                try {
                    GenerateContentResponse response = client.models.generateContent(
                        MODEL_NAME, 
                        content, 
                        GenerateContentConfig.builder().build()
                    );

                    String kiAntwort = response.text();
                    if (kiAntwort != null) {
                        String zuordnung = kiAntwort.trim();
                        System.out.println("Ergebnis: " + zuordnung);

                        // Ergebnis in der Datenbank speichern
                        Datenbankmanager.speichereZuordnung(aufnahme.id, zuordnung);
                    } else {
                        System.out.println("Keine Antwort von der KI geliefert.");
                    }
                    
                    // Da wir eventuell viele Anfragen stellen, sollten wir auch hier ein Delay einbauen, 
                    // um Rate-Limits (Quota exceeded) zu vermeiden. Aber wir warten natürlich nicht nach der
                    // letzten Anfrage.
                    if (i < aufnahmen.size() - 1) {
                        System.out.println("Warte 16 Sekunden auf nächste Anfrage, um Rate Limits zu vermeiden...");
                        Thread.sleep(16000);
                    }

                } catch (Exception e) {
                    System.err.println("\nFehler bei der KI-Anfrage für diese Aufnahme: " + e.getMessage());
                }
            }

            System.out.println("\n=== Alle Artikelzuordnungen abgeschlossen ===");

        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler bei der KI-Anfrage: " + e.getMessage());
        }
    }
}
