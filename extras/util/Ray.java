package util;

import org.joml.Vector4f;

/**
 * Created by yuliazileeva on 11/19/16.
 */
public class Ray {
    private Vector4f start;
    private Vector4f direction;

    public Ray(Vector4f s, Vector4f d) {
        this.start = s;
        this.direction = d;
    }

    public Vector4f getStart() {
        return this.start;
    }

    public Vector4f getDirection() {
        return this.direction;
    }
}
