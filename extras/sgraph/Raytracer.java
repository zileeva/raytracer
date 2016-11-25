package sgraph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import util.HitRecord;
import util.Light;
import util.Material;
import util.Ray;

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

/**
 * Created by yuliazileeva on 11/24/16.
 */
public class Raytracer {

    private INode root;

    public Raytracer(INode root) {
        this.root = root;
    }

    public void raytrace(int width, int height, Stack<Matrix4f> modelView) {
        int i, j;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {

                //get color in (r,g,b)
                Vector4f start = new Vector4f(0, 0, 0, 1);
                Vector4f direction = new Vector4f(i - width/2, j - height/2, (-0.5f * height) / (float) Math.tan(Math.toRadians(0.5f * 120.0f)), 0);

//                Matrix4f view = new Matrix4f(modelView.peek());
//                Matrix4f viewInverted = view.invert();
//                start.mul(viewInverted);
//                direction.mul(viewInverted);

                Ray ray = new Ray(start, direction);

                Color color = this.raycast(ray, modelView);

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
    private Color raycast(Ray ray, Stack<Matrix4f> modelView) {
        HitRecord hr = root.intersect(ray, modelView);
        hr.setLights(root.getLights(modelView));
//        Color color = new Color(0, 0, 0);
        Color color = new Color(0.69f, 0.8f , 0.9f);
        if (hr.isHit()) {
            color = this.shade(hr);
//            r = g = b = 255;
        }

        return color;
    }

    private Vector4f reflect(Vector4f I, Vector4f N) {
        float NdotI = N.dot(I);
        NdotI = 2.0f * NdotI;
        Vector4f r = new Vector4f(
                I.sub(N.mul(NdotI))
        );//I - 2.0 * dot(N, I) * N
        return r;
    }

    private Vector3f clamp(Vector3f val) {
        Vector3f clamped = new Vector3f(val);
        clamped.x = Math.min(Math.max(val.x, 0), 1);
        clamped.y = Math.min(Math.max(val.y, 0), 1);
        clamped.z = Math.min(Math.max(val.z, 0), 1);
        return clamped;
    }

    private Color shade(HitRecord hitRecord) {

        java.util.List<Light> lights = hitRecord.getLights();
        Color color = new Color(0, 0, 0);
        Material material = hitRecord.getMaterial();
        Vector4f position = hitRecord.getP();
        Vector4f normal = hitRecord.getNormal();

        for (int i = 0; i < lights.size(); i ++) {
            Light light = lights.get(i);

            Vector3f ambient, diffuse, specular;
            Vector4f lightVec;
            if (light.getPosition().w != 0) {
                lightVec = new Vector4f(light.getPosition().sub(position));
                lightVec = lightVec.normalize();
            } else {
                lightVec = new Vector4f(light.getPosition()).negate();
            }

            Vector4f normalView = normal.normalize();
            float nDotL = normalView.dot(lightVec);
//
//            Vector4f viewVec = new Vector4f(position).negate();
//            viewVec = viewVec.normalize();

//            Vector4f reflectVec = reflect(lightVec.negate(), normalView);
//            reflectVec = reflectVec.normalize();
//            float rDotV = Math.max(reflectVec.dot(viewVec), 0.0f);

            Vector3f materialAmbient = new Vector3f(material.getAmbient().x, material.getAmbient().y, material.getAmbient().z);
            Vector3f materialDiffuse = new Vector3f(material.getDiffuse().x, material.getDiffuse().y, material.getDiffuse().z);

            ambient = new Vector3f(materialAmbient.mul(light.getAmbient()));

            float maxNdotL = Math.max(nDotL, 0);
            Vector3f diffMulDot = new Vector3f(light.getDiffuse().mul(maxNdotL));
            diffuse = new Vector3f(materialDiffuse.mul(diffMulDot));


//            if (nDotL > 0) {
//                specular = new Vector3f(
//                        material.getSpecular().x * light.getSpecular().x * (float) Math.pow(rDotV, material.getShininess()),
//                        material.getSpecular().y * light.getSpecular().y * (float) Math.pow(rDotV, material.getShininess()),
//                        material.getSpecular().z * light.getSpecular().z * (float) Math.pow(rDotV, material.getShininess())
//                        );
//            } else {
            specular = new Vector3f(0, 0, 0);
//            }

            float spotAngle = (float) Math.cos(Math.toRadians(light.getSpotCutoff()));
            Vector4f sd = new Vector4f(light.getSpotDirection()).normalize();
            Vector4f lightVecNeg = new Vector4f(lightVec).negate();
            if (lightVecNeg.dot(sd) > spotAngle) {

                ambient = clamp(ambient);
                diffuse = clamp(diffuse);
                specular = clamp(specular);

                Color newC = new Color(
                        ambient.x + diffuse.x + specular.x,
                        ambient.y + diffuse.y + specular.y,
                        ambient.z + diffuse.z + specular.z
                );

                int r = Math.min(255, color.getRed() + newC.getRed());
                int g = Math.min(255, color.getGreen() + newC.getGreen());
                int b = Math.min(255, color.getBlue() + newC.getBlue());

                color =  new Color(r, g, b);
            }

        }
        return color;
    }
}
