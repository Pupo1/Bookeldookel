package com.bookeldookel.Etikettendrucker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class KastenErzeugerDreifach {
    public static BufferedImage erzeugeKasten(int breite, int hoehe, int linienbreite, int ps, int ds, int de, int startcode, int bilderbuchstartcode) {
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
        
        // --- Dreiteilung ---
        int drittelBreite = breite / 3;
        int linie1X = drittelBreite - linienbreite / 2;
        int linie2X = 2 * drittelBreite - linienbreite / 2;
        
        // Linie 1
        g2d.fillRect(linie1X, 0, linienbreite, hoehe);
        // Linie 2
        g2d.fillRect(linie2X, 0, linienbreite, hoehe);

        // --- Linkes Muster (startcode) ---
        int linkeBreite = linie1X - linienbreite;
        int linkesMusterKastenBreite = linkeBreite + 2 * linienbreite;
        
        BufferedImage linkesMuster = Punktmuster.erzeugeMuster(linkesMusterKastenBreite, hoehe, linienbreite, ps, ds, de, startcode);
        g2d.drawImage(linkesMuster, linienbreite, linienbreite, null);

        // --- Mittleres Muster (startcode + 1019) ---
        int mitteX = linie1X + linienbreite;
        int mitteBreite = linie2X - mitteX;
        int mittleresMusterKastenBreite = mitteBreite + 2 * linienbreite;

        BufferedImage mittleresMuster = Punktmuster.erzeugeMuster(mittleresMusterKastenBreite, hoehe, linienbreite, ps, ds, de, startcode + 1019);
        g2d.drawImage(mittleresMuster, mitteX, linienbreite, null);

        // --- Rechtes Muster (bilderbuchstartcode) ---
        int rechtsX = linie2X + linienbreite;
        int rechtsBreite = (breite - linienbreite) - rechtsX;
        int rechtesMusterKastenBreite = rechtsBreite + 2 * linienbreite;

        BufferedImage rechtesMuster = BilderbuchMuster.erzeugeMuster(rechtesMusterKastenBreite, hoehe, linienbreite, ps, ds, de, bilderbuchstartcode);
        g2d.drawImage(rechtesMuster, rechtsX, linienbreite, null);

        g2d.dispose();
        return image;
    }
}
