package com.bookeldookel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SeitenFuellerDreifach {
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
        int currentBilderbuchCode = menu.bilderbuchstartcode;

        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                // Position berechnen
                int x = menu.randlinks + col * (menu.kastenbreite + menu.horizAbstand);
                int y = menu.randoben + row * (menu.kastenhoehe + menu.vertAbstand);
                
                // Kasten erzeugen
                BufferedImage kasten = KastenErzeugerDreifach.erzeugeKasten(menu.kastenbreite, menu.kastenhoehe, menu.linienbreite, menu.ps, menu.ds, menu.de, currentCode, currentBilderbuchCode);
                
                // Nummer einblenden
                kasten = NummernEinblenderDreifach.blendeNummernEin(kasten, currentCode, currentBilderbuchCode, menu.font);
                
                // Auf das Bild zeichnen
                g2d.drawImage(kasten, x, y, null);
                
                currentCode++;
                currentBilderbuchCode++;
            }
        }
        
        g2d.dispose();
    }
}
