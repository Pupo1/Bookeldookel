package com.bookeldookel.Etikettendrucker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class BilderbuchMuster {
    public static BufferedImage erzeugeMuster(int kastenbreite, int kastenhoehe, int linienbreite, int ps, int ds, int de, int startcode) {
        // Die Logik ist ähnlich zu Punktmuster, aber verwendet BilderbuchKacheln
        // Und es gibt eine Besonderheit: BilderbuchKacheln.erzeugeKacheln liefert die 16 Kacheln.
        // Diese müssen wir nun zu einem Bild zusammensetzen.
        // Punktmuster.erzeugeMuster delegiert an EinzelMuster.erzeugeEinzelMuster, welches die Kacheln holt und zusammensetzt.
        // Wir sollten also auch eine 'BilderbuchEinzelMuster' haben oder die Logik hier einbauen.
        // Da EinzelMuster für 4x4 Kacheln zuständig ist, und BilderbuchKacheln genau 16 Kacheln (4x4) liefert,
        // ist es sinnvoll, das analog zu machen.
        
        // Da ich "möglichst neue Methoden/Klassen" erstellen soll, mache ich das hier direkt.
        
        int innerWidth = kastenbreite - 2 * linienbreite;
        int innerHeight = kastenhoehe - 2 * linienbreite;
        
        if (innerWidth <= 0 || innerHeight <= 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }

        BufferedImage image = new BufferedImage(innerWidth, innerHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, innerWidth, innerHeight);

        int squareSize = 4 * ps; // 4x4 Kacheln à ps
        
        if (squareSize > 0) {
            // Wir erzeugen das "Einzelmuster" (den 4x4 Block) für den Startcode
            BufferedImage einzelMuster = erzeugeBilderbuchEinzelMuster(ps, ds, de, startcode);
            
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

    private static BufferedImage erzeugeBilderbuchEinzelMuster(int ps, int ds, int de, int startcode) {
        int size = 4 * ps;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, size, size);

        BufferedImage[] kacheln = BilderbuchKacheln.erzeugeKacheln(ds, de, ps, startcode);

        for (int i = 0; i < 16; i++) {
            int x = (i % 4) * ps;
            int y = (i / 4) * ps;
            g2d.drawImage(kacheln[i], x, y, null);
        }

        g2d.dispose();
        return image;
    }
}
