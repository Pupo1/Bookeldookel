package com.bookeldookel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Punktkacheln {
    public static BufferedImage[] erzeugeKacheln(int ds, int de, int ps, int startcode) {
        BufferedImage[] kacheln = new BufferedImage[16];
        int[] w = new int[8];

        // 1. Startcode in 8 Zweiergruppen (W0 bis W7) zerlegen
        // Wir gehen davon aus, dass startcode in den unteren 16 Bits die Daten enthält.
        // W0 sind die Bits 15-14, W7 sind die Bits 1-0.
        for (int i = 0; i < 8; i++) {
            int shift = (7 - i) * 2;
            w[i] = (startcode >> shift) & 0x03;
        }

        // 2. Die ersten 8 Kacheln erzeugen
        for (int i = 0; i < 8; i++) {
            kacheln[i] = erzeugeEinzelKachel(ps, ds, de, w[i]);
        }

        // 3. Checksumme berechnen
        // CK = (((W1 XOR W3 XOR W4 XOR W5 ) AND 0x01) << 1) OR ((W0 XOR W2 XOR W6 XOR W7) AND 0x01)
        // Wobei Wx hier die 2-Bit Werte sind. Die Formel scheint sich auf die einzelnen Bits zu beziehen,
        // oder auf die 2-Bit Werte als Ganzes?
        // "Wobei mit den Werten W0 bis W7 die binären Zweiergruppen der ersten acht Quadrate gemeint sind."
        // Da das Ergebnis CK auch eine 2-Bit Gruppe sein soll (für das 9. Quadrat),
        // interpretiere ich die Formel so, dass wir bitweise Operationen auf den 2-Bit Werten durchführen.
        // Aber die Formel hat "AND 0x01", was nur das unterste Bit nimmt.
        // Und dann "<< 1". Das sieht so aus, als ob CK aus zwei Bits zusammengesetzt wird.
        // Bit 1 von CK: (W1^W3^W4^W5) & 1
        // Bit 0 von CK: (W0^W2^W6^W7) & 1
        // Das würde bedeuten, wir nehmen von den W-Werten jeweils nur das unterste Bit?
        // Oder sind W0..W7 hier als Indizes gemeint? Nein, "Werten W0 bis W7".
        
        // Ich nehme an, die XOR-Verknüpfung soll auf den 2-Bit Werten passieren, aber das Ergebnis wird auf 1 Bit reduziert?
        // Oder ist gemeint:
        // CK_Bit1 = (Bit0 von W1) XOR (Bit0 von W3) ... ?
        // Meistens bei solchen Codes (wie Hamming) werden Paritätsbits berechnet.
        
        // Lass uns die Formel strikt implementieren:
        // (W1 XOR W3 XOR W4 XOR W5) -> Das ist ein 2-Bit Wert.
        // AND 0x01 -> Das ist das unterste Bit dieses Ergebnisses.
        // << 1 -> Das schiebt dieses Bit auf Position 1.
        // Das ergibt das obere Bit von CK.
        
        // (W0 XOR W2 XOR W6 XOR W7) -> 2-Bit Wert.
        // AND 0x01 -> Unterstes Bit.
        // Das ergibt das untere Bit von CK.

        int ckBit1 = ((w[1] ^ w[3] ^ w[4] ^ w[5]) & 0x01) << 1;
        int ckBit0 = (w[0] ^ w[2] ^ w[6] ^ w[7]) & 0x01;
        int ck = ckBit1 | ckBit0;

        // 4. Die 9. Kachel (Index 8) ist die Checksumme
        kacheln[8] = erzeugeEinzelKachel(ps, ds, de, ck);

        // 5. Die restlichen Kacheln (Index 9 bis 15) bleiben weiß
        for (int i = 9; i < 16; i++) {
            kacheln[i] = erzeugeLeereKachel(ps);
        }

        return kacheln;
    }

    private static BufferedImage erzeugeEinzelKachel(int ps, int ds, int de, int type) {
        BufferedImage kachel = new BufferedImage(ps, ps, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = kachel.createGraphics();
        
        // Hintergrund weiß
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, ps, ps);

        g2d.setColor(Color.BLACK);

        int centerX = ps / 2;
        int centerY = ps / 2;
        int halfDs = ds / 2;

        int x = 0;
        int y = 0;

        // Typen:
        // 00 (0): Oben Links (-de, -de)
        // 01 (1): Oben Rechts (+de, -de)
        // 10 (2): Unten Rechts (+de, +de)
        // 11 (3): Unten Links (-de, +de)

        switch (type) {
            case 0: // 00
                x = centerX - de - halfDs;
                y = centerY - de - halfDs;
                break;
            case 1: // 01
                x = centerX + de - halfDs;
                y = centerY - de - halfDs;
                break;
            case 2: // 10
                x = centerX + de - halfDs;
                y = centerY + de - halfDs;
                break;
            case 3: // 11
                x = centerX - de - halfDs;
                y = centerY + de - halfDs;
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
