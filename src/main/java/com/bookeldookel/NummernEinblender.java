package com.bookeldookel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class NummernEinblender {
    public static BufferedImage blendeNummerEin(BufferedImage kasten, int nummer, int fontSize) {
        Graphics2D g2d = kasten.createGraphics();
        
        // Schriftart setzen
        Font font = new Font("SansSerif", Font.PLAIN, fontSize);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);

        // Textabmessungen berechnen für vertikale Zentrierung
        FontMetrics metrics = g2d.getFontMetrics(font);
        
        // Vertikale Position berechnen:
        // (Height - TextHeight) / 2 + Ascent
        int y = (kasten.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        int x = 5; // Kleiner Abstand vom linken Rand

        g2d.drawString(String.valueOf(nummer), x, y);
        g2d.dispose();
        
        return kasten;
    }
}
