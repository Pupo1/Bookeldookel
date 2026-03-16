package com.bookeldookel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class NummernEinblenderDreifach {
    public static BufferedImage blendeNummernEin(BufferedImage kasten, int startcode, int bilderbuchstartcode, int fontSize) {
        Graphics2D g2d = kasten.createGraphics();
        Font font = new Font("SansSerif", Font.PLAIN, fontSize);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        
        FontMetrics metrics = g2d.getFontMetrics(font);
        int textAscent = metrics.getAscent();
        int y = (kasten.getHeight() - metrics.getHeight()) / 2 + textAscent;
        
        // Linke Nummer (startcode - 5679)
        int ausgedruckterStartcode = startcode - 5679;
        int xLinks = 5;
        g2d.drawString(String.valueOf(ausgedruckterStartcode), xLinks, y);

        // Rechte Nummer (bilderbuchstartcode)
        int breite = kasten.getWidth();
        int drittelBreite = breite / 3;
        int xRechts = 2 * drittelBreite + 5;
        g2d.drawString(String.valueOf(bilderbuchstartcode), xRechts, y);

        g2d.dispose();
        return kasten;
    }
}
