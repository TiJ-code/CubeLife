package de.tij.sandboxgame;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class Configurator {
    public static boolean displayDebug = false;
    public static Vector3f viewingDistanceInChunks = new Vector3f(64.0f,2.0f,64.0f);


    public static Vector2f calcTextureMapCoords(int x, int y) {
        return new Vector2f(1.0f/(float) x,1.0f/(float) y);
    }
}
