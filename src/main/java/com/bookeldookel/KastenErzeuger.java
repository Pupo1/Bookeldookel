package com.bookeldookel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class KastenErzeuger {
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
        
        // Mittellinie
        int mitteX = breite / 2;
        int linienStartX = mitteX - (linienbreite / 2);
        g2d.fillRect(linienStartX, 0, linienbreite, hoehe);

        // Linkes Muster (jetzt startcode)
        System.out.println("Aufnahmecode:");
        int linkeBreite = linienStartX - linienbreite;
        int linkesMusterKastenBreite = linkeBreite + 2 * linienbreite;
        BufferedImage linkesMuster = Punktmuster.erzeugeMuster(linkesMusterKastenBreite, hoehe, linienbreite, ps, ds, de, startcode);
        g2d.drawImage(linkesMuster, linienbreite, linienbreite, null);

        // Rechtes Muster (jetzt startcode + 5685 + 1019)
        int rechteBreite = (breite - linienbreite) - (linienStartX + linienbreite);
        int rechtesMusterKastenBreite = rechteBreite + 2 * linienbreite;
        System.out.println("Startcode für den Abspielcode wird auf " + (startcode+1019) + " gesetzt." );
        BufferedImage rechtesMuster = Punktmuster.erzeugeMuster(rechtesMusterKastenBreite, hoehe, linienbreite, ps, ds, de, startcode +1019);
        g2d.drawImage(rechtesMuster, linienStartX + linienbreite, linienbreite, null);

        g2d.dispose();
        return image;
    }
}
