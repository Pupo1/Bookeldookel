package Testklassen;

import com.bookeldookel.Etikettendrucker.LayoutCalculator;
import com.bookeldookel.Etikettendrucker.Menu;
import com.bookeldookel.Etikettendrucker.PNG_Erzeuger;
import com.bookeldookel.Etikettendrucker.PNG_speichern;

import java.awt.image.BufferedImage;
import java.util.Scanner;

public class TestMain {
    public static void main(String[] args) {
        Menu menu = new Menu();

        // Setze alle Default-Werte manuell
        menu.bildbreite = 4880;
        menu.bildhoehe = 6680;
        menu.randlinks = 10;
        menu.randoben = 10;
        menu.randrechts = 10;
        menu.randunten = 10;
        menu.kastenbreite = 402;
        menu.kastenhoehe = 236;
        menu.horizAbstand = 10;
        menu.vertAbstand = 10;
        menu.ausgabedateiname = "out.png";
        menu.linienbreite = 1;
        menu.eingabedateiname = "codes.txt";
        menu.ps = 9;
        menu.ds = 3;
        menu.de = 1;
        menu.font = 40;
        menu.startcode = 6729;
        menu.offset = 0;

        // Frage zwingend "Code Nr." ab
        Scanner scanner = new Scanner(System.in);
        System.out.print("Code Nr. []: ");
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

        // Führe den Rest der Logik aus, wie in der originalen Main-Klasse
        MenuTest.printValues(menu);

        int[] layout = LayoutCalculator.calculate(menu);
        System.out.println("Anzahl Spalten: " + layout[0]);
        System.out.println("Anzahl Zeilen: " + layout[1]);

        BufferedImage image = PNG_Erzeuger.erzeugeBild(menu);
        
        KastenTest.test(image, menu);

        PNG_speichern.speichern(image, menu.ausgabedateiname);
        
        System.out.println("Testlauf beendet.");
    }
}
