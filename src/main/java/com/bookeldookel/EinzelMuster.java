package com.bookeldookel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class EinzelMuster {
    public static BufferedImage erzeugeEinzelMuster(int ps, int ds, int de, int startcode) {
        int size = 4 * ps;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Hintergrund weiß
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, size, size);

        BufferedImage[] kacheln = Punktkacheln.erzeugeKacheln(ds, de, ps, startcode);

        for (int i = 0; i < 16; i++) {
            int x = (i % 4) * ps;
            int y = (i / 4) * ps;
            g2d.drawImage(kacheln[i], x, y, null);
        }

        g2d.dispose();
        return image;
    }
}
