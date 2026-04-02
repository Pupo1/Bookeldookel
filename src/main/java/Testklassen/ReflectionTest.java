package Testklassen;

import com.google.genai.Client;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class ReflectionTest {
    public static void main(String[] args) {
        System.out.println("Methods of Client:");
        for (Method m : Client.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
        System.out.println("Fields of Client:");
        for (Field f : Client.class.getDeclaredFields()) {
            System.out.println(f.getName());
        }
    }
}
