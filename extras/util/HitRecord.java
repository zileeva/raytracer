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
    private float t;
    private Vector4f p;
    private List<Light> lights = new ArrayList<>();
    private Material material;
    private Vector4f normal;
    private String textureName;
    private Vector2f textureCoordinates;

    // material, point of intersection, position, light position
    public HitRecord() {
        this.hit = false;
        this.t = Float.MIN_VALUE;
    }

    public HitRecord(float t, Vector4f p, Material material, Vector4f normal, Vector2f textureCoordinates) {
        this.hit = true;
        this.t = t;
        this.p = p;
        this.material = material;
        this.normal = normal;
        this.textureCoordinates = textureCoordinates;
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

    public void addLights(List<Light> l) {
        this.lights.addAll(l);
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

    public void setTextureName(String textureName) {
        this.textureName = textureName;
    }

    public String getTextureName() {
        return textureName;
    }

    public Vector2f getTextureCoordinates() {
        return textureCoordinates;
    }
}
