package de.tij.sandboxgame;

public class Helper {

    private static Helper instance = null;

    public static Helper getInstance() {
        if (instance == null) {
            instance = new Helper();
        }

        return instance;
    }

    public Helper() {}
}