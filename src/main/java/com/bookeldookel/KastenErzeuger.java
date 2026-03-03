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
        int halbeLinienbreite = linienbreite / 2; // Um die Linie zu zentrieren, falls linienbreite > 1
        // Einfacher: Wir zeichnen die Linie so, dass sie genau in der Mitte ist.
        // Wenn linienbreite ungerade ist, ist es schwer genau zu zentrieren.
        // Wir nehmen an, die Linie beginnt bei mitteX - linienbreite/2
        int linienStartX = mitteX - (linienbreite / 2);
        g2d.fillRect(linienStartX, 0, linienbreite, hoehe);

        // Linkes Muster
        // Breite: Von linker Linie bis Mittellinie
        // StartX: linienbreite
        // EndX: linienStartX
        int linkeBreite = linienStartX - linienbreite;
        // Wir müssen ein "virtuelles" Muster erzeugen, das in diesen Bereich passt.
        // Punktmuster.erzeugeMuster erwartet die Gesamtgröße des Kastens inkl. Ränder.
        // Das ist hier etwas tricky, da Punktmuster davon ausgeht, dass es den inneren Bereich füllt.
        // Wir können Punktmuster so aufrufen, als ob es ein eigener Kasten wäre?
        // Nein, Punktmuster.erzeugeMuster nimmt kastenbreite und zieht 2*linienbreite ab.
        // Also übergeben wir:
        // kastenbreite = linkeBreite + 2*linienbreite
        // kastenhoehe = hoehe
        
        int linkesMusterKastenBreite = linkeBreite + 2 * linienbreite;
        BufferedImage linkesMuster = Punktmuster.erzeugeMuster(linkesMusterKastenBreite, hoehe, linienbreite, ps, ds, de, startcode + 41627);
        g2d.drawImage(linkesMuster, linienbreite, linienbreite, null);

        // Rechtes Muster
        // StartX: linienStartX + linienbreite
        // EndX: breite - linienbreite
        int rechteBreite = (breite - linienbreite) - (linienStartX + linienbreite);
        int rechtesMusterKastenBreite = rechteBreite + 2 * linienbreite;
        BufferedImage rechtesMuster = Punktmuster.erzeugeMuster(rechtesMusterKastenBreite, hoehe, linienbreite, ps, ds, de, startcode);
        g2d.drawImage(rechtesMuster, linienStartX + linienbreite, linienbreite, null);

        g2d.dispose();
        return image;
    }
}
