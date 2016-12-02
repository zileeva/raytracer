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


    public Raytracer(INode root, Map<String,String> textures) {
        this.root = root;
//        this.textures = (HashMap<String, String>) textures;

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

                if (i == 597 && j == 503) {
                    System.out.println("here");
                }
                //get color in (r,g,b)
                Vector4f start = new Vector4f(0, 0, 0, 1);
                Vector4f direction = new Vector4f(i - width/2, j - height/2, (-0.5f * height) / (float) Math.tan(Math.toRadians(0.5f * 120.0f)), 0);

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
//        hr.setLights(root.getLights(modelView));
        Color color;
        if (hr.isHit()) {
            color = this.shade(hr);

            String name = hr.getTextureName();
//            fColor = fColor * texture(image,fTexCoord.st);
            TextureImage textureImage = this.textures.get(name);
            Texture texture = textureImage.getTexture();

            Vector4f textureColor = clamp(textureImage.getColor(hr.getTextureCoordinates().x, hr.getTextureCoordinates().y));
            Color tc = new Color(textureColor.x, textureColor.y, textureColor.z);
            Vector4f colorToVec = textureImage.ColorToVector4f(color);
            Vector3f newColorVector = clamp(toVec3(textureColor.mul(colorToVec)));
            Color newColor = new Color(newColorVector.x, newColorVector.y, newColorVector.z);

            int r = Math.min(255, newColor.getRed());
            int g = Math.min(255, newColor.getGreen());
            int b = Math.min(255, newColor.getBlue());

            color = new Color(r, g, b);
        } else {
            color = new Color(0.69f, 0.8f , 0.9f);
        }

        return color;
    }

    private Vector4f reflect(Vector4f I, Vector4f N) {
        Vector4f i = new Vector4f(I);
        Vector4f n = new Vector4f(N);
        Vector4f r = i.sub(n.mul(2.0f * n.dot(i))); //I - 2.0 * dot(N, I) * N
        return r;
    }

    private Vector4f clamp(Vector4f val) {
        Vector4f clamped = new Vector4f(val);
        clamped.x = Math.min(Math.max(val.x, 0), 1);
        clamped.y = Math.min(Math.max(val.y, 0), 1);
        clamped.z = Math.min(Math.max(val.z, 0), 1);
        return clamped;
    }

    private Vector4f clamp255(Vector4f val) {
        Vector4f clamped = new Vector4f(val);
        clamped.x = Math.min(Math.max(val.x, 0), 255);
        clamped.y = Math.min(Math.max(val.y, 0), 255);
        clamped.z = Math.min(Math.max(val.z, 0), 255);
        return clamped;
    }

    private Vector3f clamp255(Vector3f val) {
        Vector3f clamped = new Vector3f(val);
        clamped.x = Math.min(Math.max(val.x, 0), 255);
        clamped.y = Math.min(Math.max(val.y, 0), 255);
        clamped.z = Math.min(Math.max(val.z, 0), 255);
        return clamped;
    }

    private Vector3f clamp(Vector3f val) {
        Vector3f clamped = new Vector3f(val);
        clamped.x = Math.min(Math.max(val.x, 0), 1);
        clamped.y = Math.min(Math.max(val.y, 0), 1);
        clamped.z = Math.min(Math.max(val.z, 0), 1);
        return clamped;
    }

    private Vector3f ambient(Material material, Light light) {

        Vector3f materialAmbient = toVec3(material.getAmbient());
        Vector3f ambient = new Vector3f(materialAmbient.mul(light.getAmbient()));
        return ambient;
    }

    private Vector3f diffuse(Material material, Light light, float nDotL) {

        Vector3f materialDiffuse = toVec3(material.getDiffuse());
//        diffuse = material.diffuse * light[i].diffuse * max(nDotL,0);
        Vector3f diffuse = (materialDiffuse.mul(light.getDiffuse())).mul(Math.max(nDotL, 0));
        return diffuse;
    }

    private Vector3f specular(Material material, Light light, float nDotL, float rDotV) {
        Vector3f specular;
        Vector3f materialSpecular = toVec3(material.getSpecular());

        if (nDotL > 0) {
            Vector3f specMul = new Vector3f(materialSpecular.mul(light.getSpecular()));
            float sh = (float) Math.pow(rDotV, material.getShininess());
            specular = specMul.mul(sh);
        } else {
            specular = new Vector3f(0, 0, 0);
        }

        return specular;
    }

    private Vector3f toVec3(Vector4f vec4) {
        return new Vector3f(vec4.x, vec4.y, vec4.z);
    }

    private Color shade(HitRecord hitRecord) {

//        java.util.List<Light> lights = hitRecord.getLights();

        Color color = new Color(0, 0, 0);
        Material material = hitRecord.getMaterial();
        Vector3f position = toVec3(hitRecord.getP());
        Vector3f normal = toVec3(hitRecord.getNormal()) ;

        for (int i = 0; i < this.lights.size(); i ++) {
            Light light = this.lights.get(i);

            Vector3f lightVec;
            if (light.getPosition().w != 0) {
                lightVec = toVec3(light.getPosition()).sub(toVec3(hitRecord.getP()));
                lightVec = lightVec.normalize();
            } else {
                lightVec = new Vector3f(toVec3(light.getPosition()));
                lightVec = lightVec.negate();
                lightVec = lightVec.normalize();
            }

            Vector3f normalView = toVec3(hitRecord.getNormal());
            normalView.normalize();
            float nDotL = normalView.dot(lightVec);

            Vector3f viewVec = new Vector3f(position);
            viewVec = viewVec.negate();
            viewVec = viewVec.normalize();

            Vector3f lightVecNeg = new Vector3f(lightVec);
            lightVecNeg = lightVecNeg.negate();
//            lightVecNeg = lightVecNeg.normalize();
            Vector3f reflectVec = lightVecNeg.reflect(normalView); //reflect(lightVecNeg, normalView);
            reflectVec = reflectVec.normalize();

            float rDotV = Math.max(reflectVec.dot(viewVec), 0);

            Vector3f ambient = ambient(material, light);
            Vector3f diffuse = diffuse(material, light, nDotL);
            Vector3f specular = specular(material, light, nDotL, rDotV);


            float spotAngle = (float) Math.cos(Math.toRadians(light.getSpotCutoff()));
            Vector3f spotDirection = toVec3(light.getSpotDirection());
            spotDirection = spotDirection.normalize();
            float lnDotSd = (new Vector3f(lightVec).negate()).dot(spotDirection);

            if (lnDotSd > spotAngle) {

                Vector3f ff = clamp(ambient.add(diffuse.add(specular)));

                Color newC = new Color(ff.x, ff.y, ff.z);

                int r = Math.min(255, color.getRed() + newC.getRed());
                int g = Math.min(255, color.getGreen() + newC.getGreen());
                int b = Math.min(255, color.getBlue() + newC.getBlue());

                color = new Color(r, g, b);
            }

        }
        return color;
    }
}
