package com.bookeldookel.Etikettendrucker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Einzelpunkt {
    public static BufferedImage erzeugePunkt(int ps, int ds, int de, String pattern) {
        BufferedImage image = new BufferedImage(ps, ps, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Hintergrund weiß
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, ps, ps);

        // Position holen
        int[] pos = Punktposition.getPosition();
        int offsetX = pos[0];
        int offsetY = pos[1];

        // Mittelpunkt berechnen
        int centerX = ps / 2;
        int centerY = ps / 2;
        
        // Startpunkt für das schwarze Quadrat (zentriert + offset)
        int startX = centerX - (ds / 2) + offsetX;
        int startY = centerY - (ds / 2) + offsetY;

        g2d.setColor(Color.BLACK);
        g2d.fillRect(startX, startY, ds, ds);

        g2d.dispose();
        return image;
    }
}
