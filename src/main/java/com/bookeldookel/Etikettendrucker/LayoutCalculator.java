package com.bookeldookel.Etikettendrucker;

public class LayoutCalculator {
    public static int[] calculate(Menu menu) {
        int availableWidth = menu.bildbreite - menu.randlinks - menu.randrechts;
        int cols = 0;
        if (availableWidth >= menu.kastenbreite) {
            cols = (availableWidth + menu.horizAbstand) / (menu.kastenbreite + menu.horizAbstand);
        }

        int availableHeight = menu.bildhoehe - menu.randoben - menu.randunten;
        int rows = 0;
        if (availableHeight >= menu.kastenhoehe) {
            rows = (availableHeight + menu.vertAbstand) / (menu.kastenhoehe + menu.vertAbstand);
        }

        return new int[]{cols, rows};
    }
}
