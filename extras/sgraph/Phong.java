package sgraph;

import org.joml.Vector3f;
import org.joml.Vector4f;
import util.HitRecord;
import util.Light;
import util.Material;

import java.awt.*;

/**
 * Created by yuliazileeva on 12/5/16.
 */
public class Phong {

    private ColorUtil colorUtil = new ColorUtil();

    public Phong() {}

    /**
     * Calculate color for this hitRecord and light
     * @param light
     * @param hitRecord
     * @return
     */
    public Color calculateColor(Light light, HitRecord hitRecord) {
        Material material = hitRecord.getMaterial();

        Vector3f position = toVec3(hitRecord.getP());
        Color newC = new Color(0, 0, 0);
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
        float nDotL = normalView.dot(lightVec);

        Vector3f viewVec = new Vector3f(position);
        viewVec = viewVec.negate();
        viewVec = viewVec.normalize();

        Vector3f lightVecNeg = new Vector3f(lightVec);
        lightVecNeg = lightVecNeg.negate();
        Vector3f reflectVec = lightVecNeg.reflect(normalView);
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

            Vector3f ff = colorUtil.clamp(ambient.add(diffuse.add(specular)));

            newC = new Color(ff.x, ff.y, ff.z);

        }

        return newC;
    }


    /**
     * Calculate ambient
     * @param material
     * @param light
     * @return
     */
    public Vector3f ambient(Material material, Light light) {
        Vector3f materialAmbient = toVec3(material.getAmbient());
        Vector3f ambient = new Vector3f(materialAmbient.mul(light.getAmbient()));
        return ambient;
    }

    /**
     * Calculate diffuse
     * @param material
     * @param light
     * @param nDotL
     * @return
     */
    public Vector3f diffuse(Material material, Light light, float nDotL) {
        Vector3f materialDiffuse = toVec3(material.getDiffuse());
        Vector3f diffuse = (materialDiffuse.mul(light.getDiffuse())).mul(Math.max(nDotL, 0));
        return diffuse;
    }

    /**
     * Calculate specular
     * @param material
     * @param light
     * @param nDotL
     * @param rDotV
     * @return
     */
    public Vector3f specular(Material material, Light light, float nDotL, float rDotV) {
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

}
