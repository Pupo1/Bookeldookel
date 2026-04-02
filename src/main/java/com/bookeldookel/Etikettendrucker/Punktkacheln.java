package com.bookeldookel.Etikettendrucker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Punktkacheln {
    public static BufferedImage[] erzeugeKacheln(int ds, int de, int ps, int startcode) {
        BufferedImage[] kacheln = new BufferedImage[16];

        // --- Feste Kacheln ---
        int[] centeredIndices = {0, 1, 2, 3, 4, 12};
        for (int index : centeredIndices) {
            kacheln[index] = erzeugeEinzelKachel(ps, ds, de, 4); // Case 4: Zentriert
        }
        kacheln[8] = erzeugeEinzelKachel(ps, ds, de, 5); // Case 5: Links verschoben

        // --- Daten-Kacheln ---
        int[] w = new int[8];
        StringBuilder binaryString = new StringBuilder();
        
        // 1. Startcode in 8 Zweiergruppen (W0 bis W7) zerlegen
        // Little Endian: W0 sind die Bits 1-0, W7 sind die Bits 15-14.
        // Little-Endian: Das niederwertigste Byte (LSB) kommt zuerst
        for (int i = 0; i < 8; i++) {
            w[i] = (startcode >> (i * 2)) & 0x03;

            // Bits innerhalb der Gruppe umdrehen: 0->0, 1->2, 2->1, 3->3
            int bitReversed = ((w[i] & 0x02) >> 1) | ((w[i] & 0x01) << 1);

            binaryString.append(String.format("%2s", Integer.toBinaryString(bitReversed))
                    .replace(' ', '0'));

            if (i < 7) binaryString.append(" ");
        }
        System.out.println("Little-Endian Binär: " + binaryString.toString());



        int ckBit1 = ((w[1] ^ w[7] ^ w[4] ^ w[6]) & 0x01) << 1; // die <<-Verschiebung macht diese Bit zum linken Bit (aus Big-Endian-Sichtweise)
        int ckBit0 = (w[0] ^ w[2] ^ w[3] ^ w[5]) & 0x01; // bisher nur W0,W1,W2 sicher. w4 und w7 aufgrund des Codes 006 vertauscht, sollte auch einigermaßen sicher sein, wenn es klappt.
        int ck = ckBit1 | ckBit0;

        System.out.println("CK: " + ck );

        int[] dataValues = {w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], ck};
        int[] dataTileIndices = {5, 6, 7, 9, 10, 11, 13, 14, 15};

        for (int i = 0; i < dataValues.length; i++) {
            kacheln[dataTileIndices[i]] = erzeugeEinzelKachel(ps, ds, de, dataValues[i]);
        }
        
        // Fülle restliche leere Kacheln (falls vorhanden)
        for (int i = 0; i < 16; i++) {
            if (kacheln[i] == null) {
                kacheln[i] = erzeugeLeereKachel(ps);
            }
        }

        return kacheln;
    }

    private static BufferedImage erzeugeEinzelKachel(int ps, int ds, int de, int type) {
        BufferedImage kachel = new BufferedImage(ps, ps, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = kachel.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, ps, ps);

        g2d.setColor(Color.BLACK);

        int centerX = ps / 2;
        int centerY = ps / 2;
        int halfDs = ds / 2;

        int x = centerX - halfDs;
        int y = centerY - halfDs;

        switch (type) {
            case 0: // Oben Links
                x = centerX - de - halfDs;
                y = centerY - de - halfDs;
                break;
            case 1: // Oben Rechts
                x = centerX + de - halfDs;
                y = centerY - de - halfDs;
                break;
            case 2: // Unten Rechts
                x = centerX + de - halfDs;
                y = centerY + de - halfDs;
                break;
            case 3: // Unten Links
                x = centerX - de - halfDs;
                y = centerY + de - halfDs;
                break;
            case 4: // Zentriert
                // Keine Verschiebung
                break;
            case 5: // Links
                x = centerX - de - halfDs;
                break;
        }

        g2d.fillRect(x, y, ds, ds);
        g2d.dispose();
        return kachel;
    }

    private static BufferedImage erzeugeLeereKachel(int ps) {
        BufferedImage kachel = new BufferedImage(ps, ps, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = kachel.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, ps, ps);
        g2d.dispose();
        return kachel;
    }
}
