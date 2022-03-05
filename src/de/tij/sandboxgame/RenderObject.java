package de.tij.sandboxgame;

import org.lwjgl.util.vector.Vector3f;

public abstract class RenderObject {

    protected Vector3f position = new Vector3f(0.0f,0.0f,0.0f);

    public void render() {}

    public void update(int delta) {}

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

}
