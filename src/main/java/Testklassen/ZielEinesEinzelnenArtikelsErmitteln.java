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

        // 2. Artikel aus der Datenbank laden
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

        // 4. KI-Anfrage zusammenbauen
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Du bist ein intelligenter Lager-Assistent.\n");
        promptBuilder.append("Ich habe folgenden transkribierten Text erhalten, der vermutlich einen Artikel nennt, den ich einlagern möchte:\n");
        promptBuilder.append("\"").append(aufnahme.inhalt).append("\"\n\n");
        promptBuilder.append("Hier ist eine Liste aller Artikel, die bereits in meinem System existieren:\n");
        
        for (String artikel : artikelListe) {
            promptBuilder.append("- ").append(artikel).append("\n");
        }
        
        promptBuilder.append("\nAufgabe: Zu welchem Artikel aus der Liste passt der transkribierte Text am besten?\n");
        promptBuilder.append("Bitte antworte NUR mit dem exakten Namen des passenden Artikels aus der Liste. Wenn gar kein Artikel aus der Liste passt, antworte mit 'NEUER_ARTIKEL'.");

        System.out.println("\nSende Anfrage an Gemini (" + MODEL_NAME + ") ...");

        try (Client client = Client.builder().apiKey(apiKey).build()) {
            Part textPart = Part.builder().text(promptBuilder.toString()).build();
            Content content = Content.builder().parts(Arrays.asList(textPart)).build();

            GenerateContentResponse response = client.models.generateContent(
                MODEL_NAME, 
                content, 
                GenerateContentConfig.builder().build()
            );

            String kiAntwort = response.text();
            if (kiAntwort != null) {
                String zuordnung = kiAntwort.trim();
                System.out.println("\n=== Ergebnis der Zuordnung ===");
                System.out.println(zuordnung);
                System.out.println("==============================\n");
                
                // Wir speichern das Ergebnis hier nicht ab, da es nur ein Testlauf ist
                // Datenbankmanager.speichereZuordnung(aufnahme.id, zuordnung);

            } else {
                System.out.println("Die KI hat keine Antwort geliefert.");
            }

        } catch (Exception e) {
            System.err.println("Fehler bei der KI-Anfrage: " + e.getMessage());
        }
    }
}
