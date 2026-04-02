package Testklassen;

import com.bookeldookel.Etikettendrucker.*;

import java.awt.image.BufferedImage;
import java.util.Scanner;

public class TestMainDreifach {
    public static void main(String[] args) {
        Menu menu = new Menu();

        // Setze alle Default-Werte manuell (für Dreifach-Codes optimiert, falls nötig)
        menu.bildbreite = 4880;
        menu.bildhoehe = 6680;
        menu.randlinks = 10;
        menu.randoben = 10;
        menu.randrechts = 50;
        menu.randunten = 10;
        menu.kastenbreite = 402;
        menu.kastenhoehe = 236;
        menu.horizAbstand = 10;
        menu.vertAbstand = 10;
        menu.ausgabedateiname = "out_dreifach.png";
        menu.linienbreite = 1;
        menu.eingabedateiname = "codes.txt";
        menu.ps = 9;
        menu.ds = 3;
        menu.de = 1;
        menu.font = 40; // Größere Schrift für Dreifach
        menu.startcode = 6729;
        menu.offset = 0;
        menu.bilderbuchstartcode = 1000; // Default für Bilderbuch

        Scanner scanner = new Scanner(System.in);
        
        // Frage zwingend "Code Nr." (Startcode) ab
        System.out.print("Code Nr. (Startcode) []: ");
        String codeNrInput = scanner.nextLine();
        if (!codeNrInput.isEmpty()) {
            try {
                int codeNr = Integer.parseInt(codeNrInput);
                menu.startcode = codeNr + 5685;
                System.out.println("Startcode wurde auf " + menu.startcode + " gesetzt.");
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Code Nr., Startcode bleibt " + menu.startcode);
            }
        }
        
        // Frage zwingend "Bilderbuch Startcode" ab
        System.out.print("Bilderbuch Startcode []: ");
        String bbCodeInput = scanner.nextLine();
        if (!bbCodeInput.isEmpty()) {
            try {
                int bbCode = Integer.parseInt(bbCodeInput);
                menu.bilderbuchstartcode = bbCode;
                System.out.println("Bilderbuch Startcode wurde auf " + menu.bilderbuchstartcode + " gesetzt.");
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Eingabe, Bilderbuch Startcode bleibt " + menu.bilderbuchstartcode);
            }
        }

        // Führe den Rest der Logik aus
        MenuTest.printValues(menu);

        int[] layout = LayoutCalculator.calculate(menu);
        System.out.println("Anzahl Spalten: " + layout[0]);
        System.out.println("Anzahl Zeilen: " + layout[1]);

        BufferedImage image = PNG_Erzeuger.erzeugeBild(menu);
        
        SeitenFuellerDreifach.fuelleSeite(image, menu);

        PNG_speichern.speichern(image, menu.ausgabedateiname);
        
        System.out.println("Testlauf Dreifach beendet.");
    }
}
