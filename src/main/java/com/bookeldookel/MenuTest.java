package com.bookeldookel;

public class MenuTest {
    public static void printValues(Menu menu) {
        System.out.println("\nErfasste Werte:");
        System.out.println("Bildbreite: " + menu.bildbreite);
        System.out.println("Bildhöhe: " + menu.bildhoehe);
        System.out.println("Rand links: " + menu.randlinks);
        System.out.println("Rand oben: " + menu.randoben);
        System.out.println("Rand rechts: " + menu.randrechts);
        System.out.println("Rand unten: " + menu.randunten);
        System.out.println("Kastenbreite: " + menu.kastenbreite);
        System.out.println("Kastenhöhe: " + menu.kastenhoehe);
        System.out.println("Horizontaler Abstand: " + menu.horizAbstand);
        System.out.println("Vertikaler Abstand: " + menu.vertAbstand);
        System.out.println("Ausgabedateiname: " + menu.ausgabedateiname);
        System.out.println("Rahmenbreite: " + menu.linienbreite);
        System.out.println("Eingabedatei: " + menu.eingabedateiname);
        System.out.println("Rastergröße: " + menu.ps);
        System.out.println("Punktgröße: " + menu.ds);
        System.out.println("Verschiebung: " + menu.de);
        System.out.println("Schriftgröße: " + menu.font);
    }
}
