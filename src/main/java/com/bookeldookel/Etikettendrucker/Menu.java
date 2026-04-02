package com.bookeldookel.Etikettendrucker;

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
    public int bilderbuchstartcode;
    public int modus; // Neu für die Auswahl

    public void start() {
        Scanner scanner = new Scanner(System.in);

        bildbreite = getInt(scanner, "Bildbreite", 4880);
        bildhoehe = getInt(scanner, "Bildhöhe", 6680);
        randlinks = getInt(scanner, "Rand links in Pixeln", 392);
        randoben = getInt(scanner, "Rand oben in Pixeln", 378-49); // 47 Pixel Druckerbedingter Zusatzrand
        randrechts = getInt(scanner, "Mindestrand rechts in Pixeln", 200); // 390 eigentlich laut Berechnung
        randunten = getInt(scanner, "Mindestrand unten in Pixeln", 200);// eigentlich 283
        kastenbreite = getInt(scanner, "Kastenbreite in Pixeln", 425-11); // aus irgendeinem Grund waren die korrekt bemessenen Kästen zu breit
        kastenhoehe = getInt(scanner, "Kastenhöhe in Pixeln", 260-28); // -25 waren die Kästen noch zu hoch, bei 30 zu flach
        horizAbstand = getInt(scanner, "Horizontaler Abstand zwischen den Kästen", 0);
        vertAbstand = getInt(scanner, "Vertikaler Abstand zwischen den Kästen", 0);
        ausgabedateiname = getString(scanner, "Ausgabedateiname", "out.png");
        linienbreite = getInt(scanner, "Rahmenbreite", 1);
        eingabedateiname = getString(scanner, "Eingabedatei", "codes.txt");
        ps = getInt(scanner, "Rastergröße", 9);
        ds = getInt(scanner, "Punktgröße", 3);
        de = getInt(scanner, "Verschiebung", 1);
        font = getInt(scanner, "Schriftgröße", 40);
        
        System.out.println("\nModus Auswahl:");
        System.out.println("1 = Einfach (Ein Code pro Kasten)");
        System.out.println("2 = Zweifach (Zwei Codes pro Kasten)");
        System.out.println("3 = Dreifach (Zwei Codes + Bilderbuchcode)");
        modus = getInt(scanner, "Modus", 3);

        startcode = getInt(scanner, "Startcode", 6729);
        
        if (modus == 3) {
            bilderbuchstartcode = getInt(scanner, "Bilderbuch Startcode", 15000);
        }

        offset = getInt(scanner, "Offset", 0);

        // Code Nr. Abfrage
        System.out.print("Code Nr. [optional]: ");
        String codeNrInput = scanner.nextLine();
        // wollte ich umschreiben, dass optional, klappt aber nicht:
        // falls ein Code angegeben wird, wir dder Startcode überschrieben
        // String codeNrInput = getInt(scanner, "CodeNr (optional) :", 30);
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
