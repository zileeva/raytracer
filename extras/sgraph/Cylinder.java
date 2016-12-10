package sgraph;

import org.joml.*;
import org.joml.Math;
import util.HitRecord;
import util.Material;
import util.Ray;

import java.util.Stack;
import java.util.Arrays;

/**
 * Created by yuliazileeva on 12/8/16.
 */
public class Cylinder implements IShape {
    private Material material;

    public Cylinder() {
    }

    @Override
    public void setMaterial(Material material) {
        this.material = material;
    }

    @Override
    public HitRecord intersect(Ray ray, Stack<Matrix4f> modelView) {
        Vector4f start = new Vector4f(ray.getStart());
        Vector4f direction = new Vector4f(ray.getDirection());
        HitRecord hr = new HitRecord();

        float A = direction.x * direction.x + direction.z * direction.z;
        float B = 2 * (direction.x * start.x + direction.z * start.z);
        float C = start.x * start.x + start.z * start.z - 1;

        float D = B * B - 4 * A * C;

        float t1 = (-B + (float) Math.sqrt(D)) / (2 * A);
        float t2 = (-B - (float) Math.sqrt(D)) / (2 * A);

        float t3 = (1 - start.y) / direction.y;
        float t4 = (0 - start.y) / direction.y;

        float tSideMin = Math.min(t1, t2);
        float tBaseMin = Math.min(t3, t4);

        float threshold = 0.0001f;

        if (D <= 0) return hr;

        Boolean intersected = false;
        float tMin = Float.MIN_VALUE;
        Vector4f normal = new Vector4f(0, 0, 0, 0);

        Vector4f p = new Vector4f(new Vector4f(start).add(new Vector4f(direction).mul(tSideMin)));

        // Sides
        if (p.y > 0 && p.y < 1 && tSideMin > 0) {
            normal = new Vector4f(p.x, 0, p.z, 0);
            tMin = tSideMin;
            intersected = true;
        }

        // Bases
        if (direction.y != 0) {
            p = new Vector4f(new Vector4f(start).add(new Vector4f(direction).mul(tBaseMin)));
            if (tBaseMin > 0 && (p.x * p.x + p.z * p.z - 1 <= threshold)) {
                if (Math.abs(p.y - 1) < threshold) {
                    normal = new Vector4f(0, 1, 0, 0);
                    tMin = tBaseMin;
                    intersected = true;
                } else if (Math.abs(p.y) < threshold) {
                    normal = new Vector4f(0, -1, 0, 0);
                    tMin = tBaseMin;
                    intersected = true;
                }

            }
        }

        if (intersected) {
            p = new Vector4f(new Vector4f(start).add(new Vector4f(direction).mul(tMin)));

            float theta = (float) Math.acos(p.x);
            if (theta < 0) theta += 2 * Math.PI;
            float s = theta / (float) (2 * Math.PI);

            float t = p.y / 1;

            Matrix4f normalmatrix = new Matrix4f(modelView.peek());
            normalmatrix.invert().transpose();
            normal = normalmatrix.transform(normal);
            normal = new Vector4f(new Vector3f(normal.x, normal.y, normal.z).normalize(), 0.0f);
            Matrix4f transformation = new Matrix4f(modelView.peek());
            Vector4f position = transformation.transform(p);


            Vector2f textureCoordinates = new Vector2f(-s, -t);

            hr = new HitRecord(tMin, position, this.material, normal, textureCoordinates);
        }

        return hr;
    }
}
