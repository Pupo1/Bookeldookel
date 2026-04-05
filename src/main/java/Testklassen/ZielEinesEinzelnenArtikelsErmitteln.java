package Testklassen;

import com.bookeldookel.Database.Datenbankmanager;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentConfig;

import java.util.List;
import java.util.Arrays;

public class ZielEinesEinzelnenArtikelsErmitteln {
    
    private static final String MODEL_NAME = "gemma-3-12b-it";

    public static void main(String[] args) {
        System.out.println("\n=== Starte KI-gestützte Artikelzuordnung (Testklasse - Einzelner Artikel) ===");

        // 1. Ersten passenden Eintrag suchen (check_status = 1)
        Datenbankmanager.Tonaufnahme aufnahme = Datenbankmanager.getErsteGepruefteTranskription();
        
        if (aufnahme == null) {
            System.out.println("Es wurde keine geprüfte Transkription (check_status = true) gefunden, die noch nicht zugeordnet wurde.");
            return;
        }

        System.out.println("Geprüfte Transkription gefunden: " + aufnahme.inhalt);

        // 2a. Zuerst prüfen, ob es einen direkten Match bei den Schachtel-Inhalten gibt
        List<String> schachtelInhalte = Datenbankmanager.holeAlleSchachtelInhalte();
        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("FEHLER: Die Umgebungsvariable 'BOOKELDOOKEL_API_KEY' ist nicht gesetzt.");
            return;
        }

        try (Client client = Client.builder().apiKey(apiKey).build()) {
            
            boolean inSchachtelGefunden = false;

            if (!schachtelInhalte.isEmpty()) {
                System.out.println("Prüfe, ob der Artikel direkt in einer bestehenden Schachtel untergebracht werden kann...");
                StringBuilder schachtelPromptBuilder = new StringBuilder();
                schachtelPromptBuilder.append("Du bist ein intelligenter Lager-Assistent.\n");
                schachtelPromptBuilder.append("Ich habe folgenden Artikel, den ich einlagern möchte:\n");
                schachtelPromptBuilder.append("\"").append(aufnahme.inhalt).append("\"\n\n");
                schachtelPromptBuilder.append("Hier ist eine Liste von Schachtelinhalten, die in meinem System existieren:\n");
                
                for (String inhalt : schachtelInhalte) {
                    schachtelPromptBuilder.append("- ").append(inhalt).append("\n");
                }
                
                schachtelPromptBuilder.append("\nAufgabe: Passt der neue Artikel gut zu einem dieser Schachtelinhalte?\n");
                schachtelPromptBuilder.append("Bitte antworte NUR mit dem exakten, passenden Schachtelinhalt aus der Liste. Wenn gar kein Schachtelinhalt gut passt, antworte mit 'NEIN'.");

                Part textPart = Part.builder().text(schachtelPromptBuilder.toString()).build();
                Content content = Content.builder().parts(Arrays.asList(textPart)).build();

                System.out.print("Sende Anfrage an Gemini (" + MODEL_NAME + ") für Schachtel-Check ... ");
                
                GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME, 
                    content, 
                    GenerateContentConfig.builder().build()
                );

                String kiAntwort = response.text();
                if (kiAntwort != null) {
                    String zuordnung = kiAntwort.trim();
                    if (!zuordnung.equalsIgnoreCase("NEIN")) {
                        System.out.println("\n=== Ergebnis der Zuordnung (Direkt in Schachtel) ===");
                        System.out.println("Der Artikel passt in die Schachtel mit dem Inhalt: " + zuordnung);
                        System.out.println("==============================\n");
                        // Datenbankmanager.speichereZuordnung(aufnahme.id, zuordnung); // In Testklasse auskommentiert
                        inSchachtelGefunden = true;
                    } else {
                        System.out.println("Keine passende Schachtel gefunden. Versuche nun Zuordnung über ähnliche Artikel...");
                    }
                }
                
                // Kurzes Delay, bevor wir ggf. die zweite API Anfrage machen
                Thread.sleep(16000); 
            }

            // 2b. Wenn nicht in einer Schachtel gefunden, suche nach ähnlichen Artikeln
            if (!inSchachtelGefunden) {
                List<String> artikelListe = Datenbankmanager.holeAlleArtikel();
                if (artikelListe.isEmpty()) {
                    System.out.println("Die Tabelle 'artikel' ist leer. Eine Zuordnung zu ähnlichen Artikeln ist nicht möglich.");
                    return;
                }

                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Du bist ein intelligenter Lager-Assistent.\n");
                promptBuilder.append("Ich habe folgenden Artikel, den ich einlagern möchte:\n");
                promptBuilder.append("\"").append(aufnahme.inhalt).append("\"\n\n");
                promptBuilder.append("Hier ist eine Liste aller Artikel, die bereits in meinem System existieren:\n");
                
                for (String artikel : artikelListe) {
                    promptBuilder.append("- ").append(artikel).append("\n");
                }
                
                promptBuilder.append("\nAufgabe: Zu welchem Artikel aus der Liste passt der neue Artikel am besten?\n");
                promptBuilder.append("Bitte antworte NUR mit dem exakten Namen des passenden Artikels aus der Liste. Wenn gar kein Artikel aus der Liste passt, antworte mit 'NEIN'.");

                System.out.print("Sende Anfrage an Gemini (" + MODEL_NAME + ") für Artikel-Check ... ");

                Part textPart = Part.builder().text(promptBuilder.toString()).build();
                Content content = Content.builder().parts(Arrays.asList(textPart)).build();

                GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME, 
                    content, 
                    GenerateContentConfig.builder().build()
                );

                String kiAntwort = response.text();
                if (kiAntwort != null) {
                    String aehnlicherArtikel = kiAntwort.trim();
                    System.out.println("\nKI meint, der ähnlichste Artikel ist: " + aehnlicherArtikel);
                    
                    if (!aehnlicherArtikel.equalsIgnoreCase("NEIN")) {
                        // Den Inhalt der Schachtel holen, in der dieser ähnliche Artikel liegt
                        String schachtelInhalt = Datenbankmanager.holeSchachtelInhaltZuArtikel(aehnlicherArtikel);
                        if (schachtelInhalt != null) {
                            System.out.println("=== Ergebnis der Zuordnung (Über ähnlichen Artikel) ===");
                            System.out.println("Zuzuordnender Schachtelinhalt: " + schachtelInhalt);
                            System.out.println("==============================\n");
                            // Datenbankmanager.speichereZuordnung(aufnahme.id, schachtelInhalt); // In Testklasse auskommentiert
                        } else {
                            System.out.println("Es konnte kein Schachtelinhalt für den Artikel '" + aehnlicherArtikel + "' gefunden werden.");
                        }
                    } else {
                        System.out.println("=== Ergebnis der Zuordnung ===");
                        System.out.println("NEUER_ARTIKEL");
                        System.out.println("==============================\n");
                    }
                } else {
                    System.out.println("Die KI hat keine Antwort geliefert.");
                }
            }

        } catch (Exception e) {
            System.err.println("Fehler bei der KI-Anfrage: " + e.getMessage());
        }
    }
}
