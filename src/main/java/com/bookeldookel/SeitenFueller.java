package com.bookeldookel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SeitenFueller {
    public static void fuelleSeite(BufferedImage image, Menu menu) {
        int[] layout = LayoutCalculator.calculate(menu);
        int cols = layout[0];
        int rows = layout[1];
        
        if (cols == 0 || rows == 0) {
            System.out.println("Kein Platz für Kästen.");
            return;
        }

        Graphics2D g2d = image.createGraphics();
        
        int currentCode = menu.startcode;

        // Spaltenweise füllen: Erst Zeile 0, Spalte 0, dann Zeile 1, Spalte 0...
        // Warte, "spaltenweise angeordnet" bedeutet normalerweise:
        // Kasten 1: Spalte 0, Zeile 0
        // Kasten 2: Spalte 0, Zeile 1
        // ...
        // Kasten N: Spalte 1, Zeile 0
        
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                // Position berechnen
                int x = menu.randlinks + col * (menu.kastenbreite + menu.horizAbstand);
                int y = menu.randoben + row * (menu.kastenhoehe + menu.vertAbstand);
                
                // Kasten erzeugen
                BufferedImage kasten = KastenErzeuger.erzeugeKasten(menu.kastenbreite, menu.kastenhoehe, menu.linienbreite, menu.ps, menu.ds, menu.de, currentCode);
                
                // Nummer einblenden
                kasten = NummernEinblender.blendeNummerEin(kasten, currentCode, menu.font);
                
                // Auf das Bild zeichnen
                g2d.drawImage(kasten, x, y, null);
                
                currentCode++;
            }
        }
        
        g2d.dispose();
    }
}
