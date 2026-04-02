package Testklassen;

import com.bookeldookel.Etikettendrucker.KastenErzeuger;
import com.bookeldookel.Etikettendrucker.Menu;
import com.bookeldookel.Etikettendrucker.NummernEinblender;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class KastenTest {
    public static void test(BufferedImage image, Menu menu) {
        BufferedImage kasten = KastenErzeuger.erzeugeKasten(menu.kastenbreite, menu.kastenhoehe, menu.linienbreite, menu.ps, menu.ds, menu.de, menu.startcode);
        
        // Nummer einblenden
        kasten = NummernEinblender.blendeNummerEin(kasten, menu.startcode, menu.font);

        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(kasten, menu.randlinks, menu.randoben, null);
        g2d.dispose();
    }
}
