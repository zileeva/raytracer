package sgraph;

import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;

/**
 * Created by yuliazileeva on 12/5/16.
 */
public class ColorUtil {
    public ColorUtil() {}

    /**
     * Blends given pixelColor with reflection and refraction color
     * @param pixelColor
     * @param absorption
     * @param reflectionColor
     * @param reflectivity
     * @return
     */
    public Color colorBlend(Color pixelColor, float absorption, Color reflectionColor, float reflectivity, Color refractionColor, float transparency) {
        float[] baseC = pixelColor.getRGBColorComponents(null);
        float[] rC = reflectionColor.getRGBColorComponents(null);
        float[] tC = refractionColor.getRGBColorComponents(null);

        float red = clamp(baseC[0] * absorption + rC[0] * reflectivity + tC[0] * transparency);
        float green = clamp(baseC[1] * absorption + rC[1] * reflectivity + tC[0] * transparency);
        float blue = clamp(baseC[2] * absorption + rC[2] * reflectivity + tC[0] * transparency);
        return new Color(red, green, blue);
    }

    public Vector4f colorToVector4f(Color c) {
        return new Vector4f((float) c.getRed() / 255, (float) c.getGreen() / 255, (float) c.getBlue() / 255, (float) c.getAlpha() / 255);
    }

    public float clamp(float x) {
        return Math.max(0.0f, Math.min(1.0f, x));
    }

    public Vector4f clamp(Vector4f val) {
        Vector4f clamped = new Vector4f(val);
        clamped.x = Math.min(Math.max(val.x, 0), 1);
        clamped.y = Math.min(Math.max(val.y, 0), 1);
        clamped.z = Math.min(Math.max(val.z, 0), 1);
        return clamped;
    }

    public Vector3f clamp(Vector3f val) {
        Vector3f clamped = new Vector3f(val);
        clamped.x = Math.min(Math.max(val.x, 0), 1);
        clamped.y = Math.min(Math.max(val.y, 0), 1);
        clamped.z = Math.min(Math.max(val.z, 0), 1);
        return clamped;
    }

}
