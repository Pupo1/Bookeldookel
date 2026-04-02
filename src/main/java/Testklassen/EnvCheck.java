package Testklassen;

import java.io.File;

public class EnvCheck {
    public static void main(String[] args) {
        String adcPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        String apiKey = System.getenv("BOOKELDOOKEL_API_KEY");

        System.out.println("==== Überprüfung der Umgebungsvariablen ====");
        
        System.out.println("GOOGLE_APPLICATION_CREDENTIALS = " + adcPath);
        if (adcPath != null && !adcPath.isEmpty()) {
            System.out.println(" -> Datei existiert unter dem Pfad? " + new File(adcPath).exists());
        }

        System.out.println("BOOKELDOOKEL_API_KEY = " + (apiKey != null && !apiKey.isEmpty() ? "*** IST GESETZT ***" : "NICHT GESETZT ODER LEER!"));
        
        System.out.println("\nHINWEIS:");
        System.out.println("Wenn du eine Variable in Windows gesetzt hast, aber sie hier als 'null' angezeigt wird,");
        System.out.println("musst du deine IDE (IntelliJ) einmal komplett beenden und neu starten, damit");
        System.out.println("sie die neuen Umgebungsvariablen vom Betriebssystem übernimmt.");
    }
}
