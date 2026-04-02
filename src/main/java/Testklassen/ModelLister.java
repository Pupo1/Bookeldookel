package Testklassen;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ModelLister {
    public static void main(String[] args) {
        // HIER DEINEN API-KEY EINTRAGEN
        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");
        String urlString = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;

        try {
            System.out.println("Frage verfügbare Modelle ab...");
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder result = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                rd.close();

                // Wir geben das JSON einfach direkt aus
                System.out.println("ERFOLG! Hier sind deine Modelle:");
                System.out.println(result.toString().replace("},", "},\n"));
            } else {
                System.err.println("Fehler von Google erhalten. HTTP Code: " + responseCode);
                // Fehlermeldung auslesen
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String line;
                while ((line = rd.readLine()) != null) { System.err.println(line); }
            }

        } catch (Exception e) {
            System.err.println("Verbindung fehlgeschlagen: " + e.getMessage());
        }
    }
}