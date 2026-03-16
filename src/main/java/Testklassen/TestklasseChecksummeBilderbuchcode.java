package Testklassen;

import java.util.Scanner;

public class TestklasseChecksummeBilderbuchcode {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Bitte eine 16-stellige Binärzahl eingeben: ");
        String input = scanner.nextLine().trim();

        if (input.length() != 16 || !input.matches("[01]+")) {
            System.out.println("Ungültige Eingabe. Es muss eine exakt 16-stellige Binärzahl (nur 0 und 1) sein.");
            return;
        }

        // Aufteilen der 16-stelligen Binärzahl in 8 Zweiergruppen (Big-Endian)
        // Das bedeutet, die am weitesten links stehenden Bits gehören zu W7.
        int W7 = Integer.parseInt(input.substring(0, 2), 2);
        int W6 = Integer.parseInt(input.substring(2, 4), 2);
        int W5 = Integer.parseInt(input.substring(4, 6), 2);
        int W4 = Integer.parseInt(input.substring(6, 8), 2);
        int W3 = Integer.parseInt(input.substring(8, 10), 2);
        int W2 = Integer.parseInt(input.substring(10, 12), 2);
        int W1 = Integer.parseInt(input.substring(12, 14), 2);
        int W0 = Integer.parseInt(input.substring(14, 16), 2);

        System.out.println("Aufteilung in Zweiergruppen:");
        System.out.println("W7: " + toBin(W7));
        System.out.println("W6: " + toBin(W6));
        System.out.println("W5: " + toBin(W5));
        System.out.println("W4: " + toBin(W4));
        System.out.println("W3: " + toBin(W3));
        System.out.println("W2: " + toBin(W2));
        System.out.println("W1: " + toBin(W1));
        System.out.println("W0: " + toBin(W0));

        // Checksumme berechnen
        // CK = (((W1 XOR W4 XOR W6 XOR W7) AND 0x01) << 1) OR ((W0 XOR W2 XOR W3 XOR W5) AND 0x01)
        int ckBit1 = ((W1 ^ W4 ^ W6 ^ W7) & 0x01) << 1;
        int ckBit0 = ((W0 ^ W2 ^ W3 ^ W5) & 0x01);
        int ck = ckBit1 | ckBit0;

        System.out.println("\nErrechnete Checksumme CK:");
        System.out.println("Binär: " + toBin(ck) + " (Dezimal: " + ck + ")");

        scanner.close();
    }

    private static String toBin(int val) {
        String bin = Integer.toBinaryString(val);
        if (bin.length() < 2) {
            return "0" + bin;
        }
        return bin;
    }
}