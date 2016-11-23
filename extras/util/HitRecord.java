package util;

import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Created by yuliazileeva on 11/19/16.
 */
public class HitRecord {
    private Boolean hit;
//    private Vector2f t;
    private float t;
    private Vector4f p;

    // material, point of intersection, position, light position
    public HitRecord() {
        this.hit = false;
        this.t = Float.MIN_VALUE;
    }

    public HitRecord(float t, Vector4f p) {
        this.hit = true;
        this.t = t;
        this.p = p;
//        this.hit = (t.x > 0.0f) && (t.x < t.y);
    }

    public Boolean isHit() {
        return this.hit;
    }

    public float t() { return  t; }

}
