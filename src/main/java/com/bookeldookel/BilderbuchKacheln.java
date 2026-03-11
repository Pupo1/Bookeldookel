package com.bookeldookel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class BilderbuchKacheln {
    public static BufferedImage[] erzeugeKacheln(int ds, int de, int ps, int startcode) {
        BufferedImage[] kacheln = new BufferedImage[16];

        // --- Feste Kacheln ---
        int[] centeredIndices = {0, 1, 2, 3, 4, 12};
        for (int index : centeredIndices) {
            kacheln[index] = erzeugeEinzelKachel(ps, ds, de, 4); // Case 4: Zentriert
        }
        
        // Die neunte Einzelkachel (Index 8) wird nach RECHTS verschoben (nicht links wie bei Punktkacheln)
        // Wir definieren einen neuen Typ 6 für "Rechts" in erzeugeEinzelKachel
        kacheln[8] = erzeugeEinzelKachel(ps, ds, de, 6); 

        // --- Daten-Kacheln ---
        // Die Kacheln W7-W0 ergeben aneinandergereiht den startcode in binärer Schreibweise mit Big-Endian-Notation
        // Das bedeutet W7 sind die höchsten Bits (15-14), W0 die niedrigsten (1-0).
        
        int[] w = new int[8];
        StringBuilder binaryString = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            // W7 bei i=0, W0 bei i=7
            // W7 entspricht Bits 15-14
            int shift = (7 - i) * 2;
            w[i] = (startcode >> shift) & 0x03;
            
            String bin = Integer.toBinaryString(w[i]);
            if (bin.length() < 2) {
                binaryString.append("0");
            }
            binaryString.append(bin);
        }
        System.out.println("Bilderbuch Code (Big-Endian W7..W0): " + binaryString.toString());

        // Mapping für die Checksummen-Formel:
        // Unser Array w[] enthält [W7, W6, W5, W4, W3, W2, W1, W0]
        int W7 = w[0];
        int W6 = w[1];
        int W5 = w[2];
        int W4 = w[3];
        int W3 = w[4];
        int W2 = w[5];
        int W1 = w[6];
        int W0 = w[7];

        // CK = (((W1 XOR W4 XOR W6 XOR W7) AND 0x01) << 1) OR ((W0 XOR W2 XOR W3 XOR W5) AND 0x01)
        int ckBit1 = ((W1 ^ W4 ^ W6 ^ W7) & 0x01) << 1;
        int ckBit0 = (W0 ^ W2 ^ W3 ^ W5) & 0x01;
        int ck = ckBit1 | ckBit0;
        
        // Die Kacheln w[] sind in umgekehrter Reihenfolge angeordnet, es beginnt also mit der Checksumme und geht weiter mit den Kacheln W7-W0
        // Reihenfolge: CK, W7, W6, W5, W4, W3, W2, W1, W0
        int[] dataValues = {ck, W7, W6, W5, W4, W3, W2, W1, W0};
        
        // Indizes für die Datenkacheln (gleich wie bei Punktkacheln)
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
            case 6: // Rechts (Neu für Bilderbuch)
                x = centerX + de - halfDs;
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
