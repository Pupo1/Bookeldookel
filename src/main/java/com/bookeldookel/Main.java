package com.bookeldookel;

public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.start();
        
        MenuTest.printValues(menu);

        String[] codes = FileReader.readCodes(menu.eingabedateiname);
        System.out.println("Anzahl gelesener Zeilen: " + codes.length);
    }
}
