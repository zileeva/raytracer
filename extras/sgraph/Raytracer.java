package sgraph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;
import com.jogamp.opengl.util.texture.Texture;


/**
 * Created by yuliazileeva on 11/24/16.
 */
public class Raytracer {

    private INode root;
    private HashMap<String,TextureImage> textures = new HashMap<>();
    private List<Light> lights = new ArrayList<>();
    private int MAX_RECURSION_BOUNCE = 8;
    private float REFRACTIVE_INDEX_AIR = 1.0f;
    private ColorUtil colorUtil = new ColorUtil();
    private Phong phong = new Phong();


    public Raytracer(INode root, Map<String,String> textures) {
        this.root = root;

        for (Map.Entry<String, String> t : textures.entrySet()) {
            this.addTexture(t.getKey(), t.getValue());
        }

    }

    private void addTexture(String name, String path) {
        TextureImage image = null;
        String imageFormat = path.substring(path.indexOf('.')+1);
        try {
            image = new TextureImage(path, imageFormat, name);
        } catch (IOException e) {
            throw new IllegalArgumentException("Texture "+path+" cannot be read!");
        }
        this.textures.put(name,image);
    }

    public void raytrace(int width, int height, Stack<Matrix4f> modelView) {
        int i, j;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        this.lights = root.getLights(modelView);

        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {

                if (i == width / 2 && j == height / 2) {
                    System.out.println("Percent :50%");
                }

                Vector4f start = new Vector4f(0, 0, 0, 1);
                Vector4f direction = new Vector4f(i - width/2, j - height/2, (-0.5f * height) / (float) Math.tan(Math.toRadians(0.5f * 120.0f)), 0);
                Ray ray = new Ray(start, direction);
                Color color = this.raycast(ray, modelView, 0, REFRACTIVE_INDEX_AIR);
                output.setRGB(i, j, color.getRGB());
            }
        }

        OutputStream outStream = null;

        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -output.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        output = op.filter(output, null);


        try {
            outStream = new FileOutputStream("output/raytrace.png");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }

        try {
            ImageIO.write(output, "png", outStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }
    }


    /**
     * Determines if this ray hits anything in the scene graph
     * @param ray in the view coordinate system
     * @param modelView
     */
    private Color raycast(Ray ray, Stack<Matrix4f> modelView, int bounce, float refractiveIndex) {
        HitRecord hitRecord = root.intersect(ray, modelView);
        Color color;

        if (hitRecord.isHit()) {
            color = this.shade(hitRecord, modelView);

            Color textureColor = this.textureColor(hitRecord, color);
            color = textureColor;

            float absorption = hitRecord.getMaterial().getAbsorption();
            Color reflectionColor = new Color(0, 0, 0);
            float reflection = 0;
            Color refractionColor = new Color(0, 0, 0);
            float transparency = 0;
            if (bounce <= MAX_RECURSION_BOUNCE) {

                if (hitRecord.getMaterial().getReflection() > 0) {
                    Ray reflectionRay = reflectionRay(ray, hitRecord);
                    reflection = hitRecord.getMaterial().getReflection();
                    reflectionColor = raycast(reflectionRay, modelView, bounce + 1, refractiveIndex);
                }

                if (hitRecord.getMaterial().getTransparency() > 0) {
                    Ray refractionRay = refractionRay(ray, hitRecord, refractiveIndex);
                    transparency = hitRecord.getMaterial().getTransparency();
                    refractionColor = raycast(refractionRay, modelView, bounce + 1, hitRecord.getMaterial().getRefractiveIndex());
                }

            }

            color = colorUtil.colorBlend(color, absorption, reflectionColor, reflection, refractionColor, transparency);

        } else {
            color = new Color(0.89f, 0.9f, 0.91f);
        }

        return color;
    }

    /**
     * Calculate color according to hitRecord.
     * @param hitRecord
     * @param modelView
     * @return
     */
    private Color shade(HitRecord hitRecord, Stack<Matrix4f> modelView) {

        Color color = new Color(0, 0, 0);

        for (int i = 0; i < this.lights.size(); i ++) {
            Light light = this.lights.get(i);

            if (inShadow(light, hitRecord, modelView)) {
                continue;
            } else {
                Color newColor = phong.calculateColor(light, hitRecord);

                int r = Math.min(255, color.getRed() + newColor.getRed());
                int g = Math.min(255, color.getGreen() + newColor.getGreen());
                int b = Math.min(255, color.getBlue() + newColor.getBlue());

                color = new Color(r, g, b);
            }
        }
        return color;
    }

    /**
     * Check if shadow ray hits anything. Shadow ray start at intersection point and has
     * direction of light vector
     * @param light
     * @param hitRecord
     * @param modelView
     * @return
     */
    private Boolean inShadow(Light light, HitRecord hitRecord, Stack<Matrix4f> modelView) {
        Vector4f lightVector = new Vector4f(light.getPosition()).sub(new Vector4f(hitRecord.getP()));
        lightVector.normalize();
        float fudge = 0.01f;
        Vector4f start = new Vector4f(new Vector4f(hitRecord.getP()).add(lightVector.mul(fudge)));
        Vector4f direction = new Vector4f(lightVector);
        Ray ray = new Ray(start, direction);
        HitRecord shadowHitRecord = root.intersect(ray, modelView);
        return shadowHitRecord.isHit();
    }

    /**
     * Calculate color for this hitRecord accoring to texture
     * @param hitRecord
     * @param color
     * @return
     */
    private Color textureColor(HitRecord hitRecord, Color color) {
        String name = hitRecord.getTextureName();
        TextureImage textureImage = this.textures.get(name);
        Vector4f textureColor = colorUtil.clamp(textureImage.getColor(hitRecord.getTextureCoordinates().x, hitRecord.getTextureCoordinates().y));
        Vector4f colorToVec = colorUtil.colorToVector4f(color);
        Vector3f newColorVector = colorUtil.clamp(toVec3(textureColor.mul(colorToVec)));
        Color newColor = new Color(newColorVector.x, newColorVector.y, newColorVector.z);

        int r = Math.min(255, newColor.getRed());
        int g = Math.min(255, newColor.getGreen());
        int b = Math.min(255, newColor.getBlue());

        return new Color(r, g, b);

    }

    /**
     * Calculate refractive ray
     * @param ray
     * @param hitRecord
     * @param refractiveIndex_i
     * @return
     */
    private Ray refractionRay(Ray ray, HitRecord hitRecord, float refractiveIndex_i) {
        float refractiveIndex = hitRecord.getMaterial().getRefractiveIndex();
        Vector4f rayDirection = new Vector4f(ray.getDirection());
        rayDirection = rayDirection.normalize();
        Vector4f normal = new Vector4f(hitRecord.getNormal());
        float nDotI = normal.dot(rayDirection);
        float snellLaw = refractiveIndex_i / refractiveIndex;
        float cosTheta_i = - nDotI;
        float cosTheta_t = (float) Math.sqrt(1 - (snellLaw * snellLaw) * (1 - nDotI * nDotI));
        Vector4f rv1 = new Vector4f(new Vector4f(rayDirection.add(normal.mul(cosTheta_i))).mul(snellLaw));
        Vector4f rv2 = new Vector4f(normal.mul(cosTheta_t));
        Vector4f refractionVector = new Vector4f(rv1.sub(rv2));
        float fudge = 0.01f;
        Vector4f start = new Vector4f(hitRecord.getP()).add(refractionVector.mul(fudge));
        Vector4f direction = new Vector4f(refractionVector);
        Ray refractionRay = new Ray(start, direction);
        return refractionRay;
    }

    /**
     * Calculate reflection ray
     * @param ray
     * @param hitRecord
     * @return
     */
    private Ray reflectionRay(Ray ray, HitRecord hitRecord) {
        Vector4f normal = new Vector4f(hitRecord.getNormal());
        Vector4f rayDirection = new Vector4f(ray.getDirection());
        rayDirection = rayDirection.normalize();
        Vector3f reflect = toVec3(rayDirection).reflect(toVec3(normal));
        Vector4f reflectionVector = new Vector4f(reflect, 0);
        reflectionVector.normalize();
        float fudge = 0.01f;
        Vector4f start = new Vector4f(hitRecord.getP()).add(reflectionVector.mul(fudge));
        Vector4f direction = new Vector4f(reflectionVector);
        Ray reflectionRay = new Ray(start, direction);
        return reflectionRay;
    }

    private Vector3f toVec3(Vector4f vec4) {
        return new Vector3f(vec4.x, vec4.y, vec4.z);
    }

}