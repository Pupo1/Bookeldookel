package Testklassen;

import com.bookeldookel.Database.Transkript;

public class Transkriptionstest {
    public static void main(String[] args) {
        Transkript.initDatabase();
        Transkript.transkribiereVerzeichnis();
    }
}
