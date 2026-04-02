package com.bookeldookel.Etikettendrucker;

import Testklassen.MenuTest;

import java.awt.image.BufferedImage;

public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.start();
        
        MenuTest.printValues(menu);

        int[] layout = LayoutCalculator.calculate(menu);
        System.out.println("Anzahl Spalten: " + layout[0]);
        System.out.println("Anzahl Zeilen: " + layout[1]);

        BufferedImage image = PNG_Erzeuger.erzeugeBild(menu);
        
        // Verzweigung je nach Modus
        if (menu.modus == 1) {
            // Für Modus 1 (Einfach) müssen wir einen passenden SeitenFueller erstellen oder den KastenErzeugerUni nutzen
            // Da wir noch keinen SeitenFuellerUni haben, iterieren wir hier schnell oder rufen eine Hilfsmethode auf.
            fuelleSeiteUni(image, menu);
        } else if (menu.modus == 2) {
            SeitenFueller.fuelleSeite(image, menu);
        } else {
            // Default Modus 3
            SeitenFuellerDreifach.fuelleSeite(image, menu);
        }

        PNG_speichern.speichern(image, menu.ausgabedateiname);
    }

    private static void fuelleSeiteUni(BufferedImage image, Menu menu) {
        int[] layout = LayoutCalculator.calculate(menu);
        int cols = layout[0];
        int rows = layout[1];
        
        if (cols == 0 || rows == 0) return;

        java.awt.Graphics2D g2d = image.createGraphics();
        int currentCode = menu.startcode;

        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                int x = menu.randlinks + col * (menu.kastenbreite + menu.horizAbstand);
                int y = menu.randoben + row * (menu.kastenhoehe + menu.vertAbstand);
                
                BufferedImage kasten = KastenErzeugerUni.erzeugeKasten(menu.kastenbreite, menu.kastenhoehe, menu.linienbreite, menu.ps, menu.ds, menu.de, currentCode);
                kasten = NummernEinblender.blendeNummerEin(kasten, currentCode, menu.font);
                
                g2d.drawImage(kasten, x, y, null);
                currentCode++;
            }
        }
        g2d.dispose();
    }
    public class TextMitPause {

        public static void main(String[] args) {
            // Erster mehrzeiliger Text
            System.out.println("Sende Prompt...\n"
                    + "Das ist ein mehrzeiliger Text.\n"
                    + "Die Ausgabe erfolgt sofort.");

            // 10 Sekunden warten (10000 Millisekunden)
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("Die Wartezeit wurde unterbrochen.");
            }

            // Zweiter mehrzeiliger Text
            System.out.println("\nJetzt sind 10 Sekunden vergangen!\n"
                    + "Hier ist der zweite mehrzeilige Text.\n"
                    + "Programm beendet.");
        }
    }
}
