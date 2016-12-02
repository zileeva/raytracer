package sgraph;

import org.joml.Matrix4f;
import util.HitRecord;

import java.util.Stack;

import util.Material;
import util.Ray;

/**
 * Created by yuliazileeva on 12/1/16.
 */
public interface IShape {
    /**
     * Finds intersection hit record between ray and sphere if it exists
     * @param ray in view coordinate system
     * @param modelView
     * @return
     */
    HitRecord intersect(Ray ray, Stack<Matrix4f> modelView);

    /**
     * Sets material for this object
     * @param material
     */
    void setMaterial(Material material);
}
