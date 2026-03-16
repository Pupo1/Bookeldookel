package com.bookeldookel;

import java.util.Scanner;

public class Menu {
    public int bildbreite;
    public int bildhoehe;
    public int randlinks;
    public int randoben;
    public int randrechts;
    public int randunten;
    public int kastenbreite;
    public int kastenhoehe;
    public int horizAbstand;
    public int vertAbstand;
    public String ausgabedateiname;
    public int linienbreite;
    public String eingabedateiname;
    public int ps;
    public int ds;
    public int de;
    public int font;
    public int startcode;
    public int offset;
    public int bilderbuchstartcode; // Neu

    public void start() {
        Scanner scanner = new Scanner(System.in);

        bildbreite = getInt(scanner, "Bildbreite", 4880);
        bildhoehe = getInt(scanner, "Bildhöhe", 6680);
        randlinks = getInt(scanner, "Rand links in Pixeln", 10);
        randoben = getInt(scanner, "Rand oben in Pixeln", 10);
        randrechts = getInt(scanner, "Mindestrand rechts in Pixeln", 10);
        randunten = getInt(scanner, "Mindestrand unten in Pixeln", 10);
        kastenbreite = getInt(scanner, "Kastenbreite in Pixeln", 402);
        kastenhoehe = getInt(scanner, "Kastenhöhe in Pixeln", 236);
        horizAbstand = getInt(scanner, "Horizontaler Abstand zwischen den Kästen", 10);
        vertAbstand = getInt(scanner, "Vertikaler Abstand zwischen den Kästen", 10);
        ausgabedateiname = getString(scanner, "Ausgabedateiname", "out.png");
        linienbreite = getInt(scanner, "Rahmenbreite", 1);
        eingabedateiname = getString(scanner, "Eingabedatei", "codes.txt");
        ps = getInt(scanner, "Rastergröße", 9);
        ds = getInt(scanner, "Punktgröße", 3);
        de = getInt(scanner, "Verschiebung", 1);
        font = getInt(scanner, "Schriftgröße", 40);
        startcode = getInt(scanner, "Startcode", 6729);
        bilderbuchstartcode = getInt(scanner, "Bilderbuch Startcode", 0); // Neu
        offset = getInt(scanner, "Offset", 0);

        // Code Nr. Abfrage
        System.out.print("Code Nr. []: ");
        String codeNrInput = scanner.nextLine();
        if (!codeNrInput.isEmpty()) {
            try {
                int codeNr = Integer.parseInt(codeNrInput);
                startcode = codeNr + 5685;
                System.out.println("Startcode wurde auf " + startcode + " gesetzt.");
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Code Nr., Startcode bleibt " + startcode);
            }
        }
    }

    private int getInt(Scanner scanner, String prompt, int defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine();
        if (input.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Ungültige Eingabe, verwende Standardwert.");
            return defaultValue;
        }
    }

    private String getString(Scanner scanner, String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine();
        if (input.isEmpty()) {
            return defaultValue;
        }
        return input;
    }
}
