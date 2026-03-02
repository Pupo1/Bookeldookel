package com.bookeldookel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class PNG_Erzeuger {
    public static BufferedImage erzeugeBild(Menu menu) {
        BufferedImage image = new BufferedImage(menu.bildbreite, menu.bildhoehe, BufferedImage.TYPE_INT_RGB);
        
        // Hintergrund weiß füllen
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, menu.bildbreite, menu.bildhoehe);
        g2d.dispose();

        return image;
    }
}
