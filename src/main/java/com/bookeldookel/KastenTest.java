package com.bookeldookel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class KastenTest {
    public static void test(BufferedImage image, Menu menu) {
        BufferedImage kasten = KastenErzeuger.erzeugeKasten(menu.kastenbreite, menu.kastenhoehe, menu.linienbreite);
        
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(kasten, menu.randlinks, menu.randoben, null);
        g2d.dispose();
    }
}
