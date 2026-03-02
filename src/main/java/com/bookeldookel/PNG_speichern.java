package com.bookeldookel;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class PNG_speichern {

    public static void speichern(BufferedImage image, String dateiname) {
        try {
            // Konvertierung zu 1-Bit (Schwarz/Weiß)
            BufferedImage binaryImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g2d = binaryImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            // Pfad zum Resources-Ordner
            File outputFile = new File("src/main/resources/" + dateiname);
            
            // Writer für PNG holen
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (!writers.hasNext()) throw new IllegalStateException("Kein PNG-Writer gefunden");
            ImageWriter writer = writers.next();

            try (ImageOutputStream stream = ImageIO.createImageOutputStream(outputFile)) {
                writer.setOutput(stream);
                
                ImageWriteParam param = writer.getDefaultWriteParam();
                ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);
                IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, param);

                // DPI setzen (600 DPI)
                setDPI(metadata);

                writer.write(metadata, new IIOImage(binaryImage, null, metadata), param);
            }
            
            System.out.println("Bild erfolgreich gespeichert unter: " + outputFile.getAbsolutePath());
            writer.dispose();

        } catch (IOException e) {
            System.err.println("Fehler beim Speichern des Bildes: " + e.getMessage());
        }
    }

    private static void setDPI(IIOMetadata metadata) throws IOException {
        // 600 DPI in Millimeter pro Pixel umrechnen
        // 1 Inch = 25.4 mm
        // Pixelgröße in mm = 25.4 / DPI
        double dotsPerMillimeter = 600.0 / 25.4;
        double pixelSizeMM = 1.0 / dotsPerMillimeter;

        String metadataFormat = "javax_imageio_1.0";
        IIOMetadataNode root = new IIOMetadataNode(metadataFormat);
        
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
        IIOMetadataNode horizontal = new IIOMetadataNode("HorizontalPixelSize");
        horizontal.setAttribute("value", Double.toString(pixelSizeMM));
        dimension.appendChild(horizontal);

        IIOMetadataNode vertical = new IIOMetadataNode("VerticalPixelSize");
        vertical.setAttribute("value", Double.toString(pixelSizeMM));
        dimension.appendChild(vertical);

        root.appendChild(dimension);
        metadata.mergeTree(metadataFormat, root);
    }
}
