package com.bookeldookel.Database;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentConfig;

import java.util.List;
import java.util.Arrays;
import java.util.Scanner;

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

        // 2. Artikel und Schachtel-Inhalte aus der Datenbank laden
        List<String> artikelListe = Datenbankmanager.holeAlleArtikel();
        List<String> schachtelInhalte = Datenbankmanager.holeAlleSchachtelInhalte();

        if (artikelListe.isEmpty() && schachtelInhalte.isEmpty()) {
            System.out.println("Die Tabellen 'artikel' und 'schachtel' enthalten keine Daten. Eine Zuordnung ist nicht möglich.");
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
            
            // Schleife über alle Aufnahmen
            for (int i = 0; i < aufnahmen.size(); i++) {
                Datenbankmanager.Tonaufnahme aufnahme = aufnahmen.get(i);
                System.out.println("\n--------------------------------------------------");
                System.out.println("Verarbeite Aufnahme ID " + aufnahme.id + ": \"" + aufnahme.inhalt + "\"");

                boolean inSchachtelGefunden = false;

                // --- SCHRITT A: Direkter Vergleich mit Schachtel-Inhalten ---
                if (!schachtelInhalte.isEmpty()) {
                    System.out.println("-> Prüfe, ob der Artikel direkt zu einem Schachtel-Inhalt passt...");
                    
                    StringBuilder schachtelPromptBuilder = new StringBuilder();
                    schachtelPromptBuilder.append("Du bist ein intelligenter Lager-Assistent.\n");
                    schachtelPromptBuilder.append("Ich habe folgenden neuen Artikel, den ich einlagern möchte:\n");
                    schachtelPromptBuilder.append("\"").append(aufnahme.inhalt).append("\"\n\n");
                    schachtelPromptBuilder.append("Hier ist eine Liste von Beschreibungen, was sich in meinen Schachteln befindet:\n");
                    
                    for (String inhalt : schachtelInhalte) {
                        schachtelPromptBuilder.append("- ").append(inhalt).append("\n");
                    }
                    
                    schachtelPromptBuilder.append("\nAufgabe: Passt der neue Artikel inhaltlich gut zu einer dieser Schachteln?\n");
                    schachtelPromptBuilder.append("Bitte antworte NUR mit der exakten Beschreibung der passenden Schachtel aus der Liste. Wenn gar keine Schachtel passt, antworte mit 'NEIN'.");

                    try {
                        Part textPart = Part.builder().text(schachtelPromptBuilder.toString()).build();
                        Content content = Content.builder().parts(Arrays.asList(textPart)).build();

                        GenerateContentResponse response = client.models.generateContent(
                            MODEL_NAME, 
                            content, 
                            GenerateContentConfig.builder().build()
                        );

                        String kiAntwort = response.text();
                        if (kiAntwort != null) {
                            String zuordnung = kiAntwort.trim();
                            if (!zuordnung.equalsIgnoreCase("NEIN")) {
                                System.out.println("-> KI Ergebnis (Schachtel direkt): " + zuordnung);
                                Integer schachtelId = Datenbankmanager.getSchachtelIdByInhalt(zuordnung);
                                Datenbankmanager.speichereZuordnung(aufnahme.id, zuordnung, schachtelId);
                                inSchachtelGefunden = true;
                            } else {
                                System.out.println("-> KI meint: Passt in keine bestehende Schachtel direkt.");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Fehler bei der KI-Anfrage (Schachtel-Check): " + e.getMessage());
                    }

                    // Puffer, falls wir gleich noch eine Anfrage machen müssen
                    if (!inSchachtelGefunden && !artikelListe.isEmpty()) {
                        System.out.println("Warte 16 Sekunden wegen Rate Limits...");
                        Thread.sleep(16000);
                    }
                }

                // --- SCHRITT B: Suche nach ähnlichem Artikel ---
                if (!inSchachtelGefunden && !artikelListe.isEmpty()) {
                    System.out.println("-> Prüfe, ob es einen ähnlichen Einzelartikel gibt...");
                    
                    StringBuilder artikelPromptBuilder = new StringBuilder();
                    artikelPromptBuilder.append("Du bist ein intelligenter Lager-Assistent.\n");
                    artikelPromptBuilder.append("Ich habe folgenden neuen Artikel, den ich einlagern möchte:\n");
                    artikelPromptBuilder.append("\"").append(aufnahme.inhalt).append("\"\n\n");
                    artikelPromptBuilder.append("Hier ist eine Liste von Artikeln, die bereits in meinem System existieren:\n");
                    
                    for (String artikel : artikelListe) {
                        artikelPromptBuilder.append("- ").append(artikel).append("\n");
                    }
                    
                    artikelPromptBuilder.append("\nAufgabe: Zu welchem Artikel aus der Liste passt der neue Artikel am besten?\n");
                    artikelPromptBuilder.append("Bitte antworte NUR mit dem exakten Namen des passenden Artikels aus der Liste. Wenn gar kein Artikel aus der Liste passt, antworte mit 'NEIN'.");

                    System.out.print("Sende Anfrage an Gemini (" + MODEL_NAME + ") für Artikel-Check ... ");

                    try {
                        Part textPart = Part.builder().text(artikelPromptBuilder.toString()).build();
                        Content content = Content.builder().parts(Arrays.asList(textPart)).build();

                        GenerateContentResponse response = client.models.generateContent(
                            MODEL_NAME, 
                            content, 
                            GenerateContentConfig.builder().build()
                        );

                        String kiAntwort = response.text();
                        if (kiAntwort != null) {
                            String aehnlicherArtikel = kiAntwort.trim();
                            
                            if (!aehnlicherArtikel.equalsIgnoreCase("NEIN")) {
                                System.out.println("\n-> KI meint, der ähnlichste Artikel ist: " + aehnlicherArtikel);
                                
                                // Neue Logik: Suche alle Artikel, die diese Zeichenkette enthalten
                                List<Datenbankmanager.Artikel> gefundeneArtikel = Datenbankmanager.sucheArtikelMitAehnlicherBezeichnung(aehnlicherArtikel);
                                
                                if (gefundeneArtikel.isEmpty()) {
                                    System.out.println("-> Keine passenden Artikel in der Datenbank gefunden.");
                                    Datenbankmanager.speichereZuordnung(aufnahme.id, "NEUER_ARTIKEL", null);
                                } else if (gefundeneArtikel.size() == 1) {
                                    // Genau ein Treffer -> automatisch auswählen
                                    Datenbankmanager.Artikel ausgewaehlterArtikel = gefundeneArtikel.get(0);
                                    System.out.println("-> Ein passender Artikel gefunden: " + ausgewaehlterArtikel.bezeichnung);
                                    System.out.println("-> Automatisch zugeordnet zu Schachtel-ID: " + ausgewaehlterArtikel.schachtelId);
                                    Datenbankmanager.speichereZuordnung(aufnahme.id, ausgewaehlterArtikel.bezeichnung, ausgewaehlterArtikel.schachtelId);
                                } else {
                                    // Mehrere Treffer -> Auswahlmenu
                                    System.out.println("-> Mehrere passende Artikel gefunden. Bitte auswählen:");
                                    for (int j = 0; j < gefundeneArtikel.size(); j++) {
                                        Datenbankmanager.Artikel a = gefundeneArtikel.get(j);
                                        System.out.println("  [" + j + "] " + a.bezeichnung + " (Schachtel-ID: " + (a.schachtelId != null ? a.schachtelId : "keine") + ")");
                                    }
                                    System.out.print("Auswahl (Nummer eingeben): ");
                                    Scanner auswahlScanner = new Scanner(System.in);
                                    String auswahl = auswahlScanner.nextLine().trim();
                                    try {
                                        int index = Integer.parseInt(auswahl);
                                        if (index >= 0 && index < gefundeneArtikel.size()) {
                                            Datenbankmanager.Artikel ausgewaehlterArtikel = gefundeneArtikel.get(index);
                                            System.out.println("-> Ausgewählt: " + ausgewaehlterArtikel.bezeichnung);
                                            System.out.println("-> Zugeordnet zu Schachtel-ID: " + ausgewaehlterArtikel.schachtelId);
                                            Datenbankmanager.speichereZuordnung(aufnahme.id, ausgewaehlterArtikel.bezeichnung, ausgewaehlterArtikel.schachtelId);
                                        } else {
                                            System.out.println("-> Ungültige Auswahl. Eintrag wird übersprungen.");
                                            Datenbankmanager.speichereZuordnung(aufnahme.id, "NEUER_ARTIKEL", null);
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println("-> Ungültige Eingabe. Eintrag wird übersprungen.");
                                        Datenbankmanager.speichereZuordnung(aufnahme.id, "NEUER_ARTIKEL", null);
                                    }
                                }
                            } else {
                                System.out.println("\n-> KI meint: Kein ähnlicher Artikel gefunden.");
                                Datenbankmanager.speichereZuordnung(aufnahme.id, "NEUER_ARTIKEL", null);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Fehler bei der KI-Anfrage (Artikel-Check): " + e.getMessage());
                    }
                } else if (!inSchachtelGefunden) {
                    // Weder Schachtel noch Artikel Liste vorhanden/durchsuchbar
                    System.out.println("-> Keine Vergleichsdaten (Artikel) vorhanden.");
                    Datenbankmanager.speichereZuordnung(aufnahme.id, "NEUER_ARTIKEL", null);
                }
                
                // Wir haben die Funktionalität so belassen, dass wir auch in der Liste der Artikel schauen,
                // wenn das Suchen nach dem Schachtelinhalt kein Ergebnis liefert. Und wir haben
                // die Sleep-Pausen zwischen den KI-Aufrufen erhalten, da sonst Rate-Limits greifen.
                
                // Delay vor dem nächsten Schleifendurchlauf (nächste Aufnahme)
                if (i < aufnahmen.size() - 1) {
                    System.out.println("Warte 16 Sekunden auf nächste Aufnahme...");
                    try {
                        Thread.sleep(16000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            System.out.println("\n=== Alle Artikelzuordnungen abgeschlossen ===");

        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler bei der KI-Anfrage: " + e.getMessage());
        }
    }
}
