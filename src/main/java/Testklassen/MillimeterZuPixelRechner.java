package Testklassen;

import java.util.Scanner;

public class MillimeterZuPixelRechner {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        final double DPI = 600.0;
        final double MM_PER_INCH = 25.4;

        System.out.println("Millimeter zu Pixel Rechner (bei " + (int)DPI + " DPI)");
        System.out.println("Geben Sie 'Ende' ein, um das Programm zu beenden.");

        while (true) {
            System.out.print("Länge in mm: ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("Ende")) {
                break;
            }

            try {
                // Kommas durch Punkte ersetzen, falls der Benutzer deutsche Schreibweise nutzt
                input = input.replace(',', '.');
                double mm = Double.parseDouble(input);
                
                double pixels = (mm * DPI) / MM_PER_INCH;
                long roundedPixels = Math.round(pixels);

                System.out.println(mm + " mm entsprechen ca. " + roundedPixels + " Pixeln.");
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Eingabe. Bitte eine Zahl oder 'Ende' eingeben.");
            }
        }
        
        System.out.println("Programm beendet.");
        scanner.close();
    }
}
