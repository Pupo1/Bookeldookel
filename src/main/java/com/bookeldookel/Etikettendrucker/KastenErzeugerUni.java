package com.bookeldookel.Etikettendrucker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class KastenErzeugerUni {
    public static BufferedImage erzeugeKasten(int breite, int hoehe, int linienbreite, int ps, int ds, int de, int startcode) {
        BufferedImage image = new BufferedImage(breite, hoehe, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Hintergrund weiß füllen
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, breite, hoehe);

        // Rahmen schwarz zeichnen
        g2d.setColor(Color.BLACK);
        
        // Oben
        g2d.fillRect(0, 0, breite, linienbreite);
        // Unten
        g2d.fillRect(0, hoehe - linienbreite, breite, linienbreite);
        // Links
        g2d.fillRect(0, 0, linienbreite, hoehe);
        // Rechts
        g2d.fillRect(breite - linienbreite, 0, linienbreite, hoehe);

        // Punktmuster einfügen
        BufferedImage muster = Punktmuster.erzeugeMuster(breite, hoehe, linienbreite, ps, ds, de, startcode);
        g2d.drawImage(muster, linienbreite, linienbreite, null);

        g2d.dispose();
        return image;
    }
}
