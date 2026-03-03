package com.bookeldookel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Punktmuster {
    public static BufferedImage erzeugeMuster(int kastenbreite, int kastenhoehe, int linienbreite, int ps, int ds, int de, int startcode) {
        int innerWidth = kastenbreite - 2 * linienbreite;
        int innerHeight = kastenhoehe - 2 * linienbreite;
        
        if (innerWidth <= 0 || innerHeight <= 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }

        BufferedImage image = new BufferedImage(innerWidth, innerHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Hintergrund weiß
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, innerWidth, innerHeight);

        int squareSize = 4 * ps; // Jetzt 4x4 Kacheln
        
        if (squareSize > 0) {
            BufferedImage einzelMuster = EinzelMuster.erzeugeEinzelMuster(ps, ds, de, startcode);
            
            int cols = innerWidth / squareSize;
            int rows = innerHeight / squareSize;

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    g2d.drawImage(einzelMuster, x * squareSize, y * squareSize, null);
                }
            }
        }

        g2d.dispose();
        return image;
    }
}
