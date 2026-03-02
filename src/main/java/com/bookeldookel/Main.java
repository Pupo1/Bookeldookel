package com.bookeldookel;

public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.start();
        
        MenuTest.printValues(menu);

        int[] layout = LayoutCalculator.calculate(menu);
        System.out.println("Anzahl Spalten: " + layout[0]);
        System.out.println("Anzahl Zeilen: " + layout[1]);
    }
}
