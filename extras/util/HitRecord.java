package util;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuliazileeva on 11/19/16.
 */
public class HitRecord {
    private Boolean hit;
//    private Vector2f t;
    private float t;
    private Vector4f p;
    private List<Light> lights;
    private Material material;
    private Vector4f normal;

    // material, point of intersection, position, light position
    public HitRecord() {
        this.hit = false;
        this.t = Float.MIN_VALUE;
        this.lights = new ArrayList<>();
    }

    public HitRecord(float t, Vector4f p, Material material, Vector4f normal) {
        this.hit = true;
        this.t = t;
        this.p = p;
        this.material = material;
        this.normal = normal;
//        this.hit = (t.x > 0.0f) && (t.x < t.y);
    }

    public Boolean isHit() {
        return this.hit;
    }

    public float t() { return  t; }

    public Vector4f getP() {
        return p;
    }

    public Vector4f getNormal() {
        return normal;
    }

    public List<Light> getLights() {
        return this.lights;
    }

    public void setLights(List<Light> lights) {
        this.lights = lights;
    }

    public Material getMaterial() {
        return material;
    }
}
