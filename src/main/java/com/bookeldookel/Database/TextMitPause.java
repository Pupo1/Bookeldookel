package com.bookeldookel.Database;

public class TextMitPause {

    public static void main(String[] args) {
        // Erster mehrzeiliger Text
        System.out.println("Sende Prompt an Gemini Flash 2.5...:\n"
                         + "Suche aus der angehängten Datei den Ort, an dem du am ehesten den folgenden Artikel einordnen würdest:\n"
                         + "Spektralanalysegerät");

        // 10 Sekunden warten (10000 Millisekunden)
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            System.out.println("Die Wartezeit wurde unterbrochen.");
        }

        // Zweiter mehrzeiliger Text
        System.out.println("\nJetzt sind 10 Sekunden vergangen!\n"
                         + "Hier ist der zweite mehrzeilige Text.\n"
                         + "Programm beendet.");
    }
}