package sgraph;

import org.joml.*;
import org.joml.Math;
import util.HitRecord;
import util.Material;
import util.Ray;

import java.util.Stack;

/**
 * Created by yuliazileeva on 12/1/16.
 */
public class Box implements IShape {

    private Material material;

    public Box() {

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

        float tx_1 = (-0.5f - start.x) / direction.x;
        float ty_1 = (-0.5f - start.y) / direction.y;
        float tz_1 = (-0.5f - start.z) / direction.z;

        float tx_2 = (0.5f - start.x) / direction.x;
        float ty_2 = (0.5f - start.y) / direction.y;
        float tz_2 = (0.5f - start.z) / direction.z;


        float tMax = Math.max( Math.max( Math.min(tx_1, tx_2), Math.min(ty_1, ty_2)), Math.min(tz_1, tz_2) );
        float tMin = Math.min( Math.min( Math.max(tx_1, tx_2), Math.max(ty_1, ty_2)), Math.max(tz_1, tz_2) );

        if ((tMin >= 0) && (tMax < tMin)) {

            Vector4f p = new Vector4f(start.add(direction.mul(tMax)));
            Vector4f normal = new Vector4f(0, 0, 0, 0);

            float threshold = 0.0001f;

            // Might be vertically flipped
            float s = 0, t = 0;
            if (Math.abs(p.z - 0.5f) < threshold) { //front
                s = 0.75f + (0.5f - p.x) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
                normal.z = 1;
            } else if (Math.abs(p.z + 0.5f) < threshold) { //back
                s = 0.25f + (0.5f - p.x) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
                normal.z = -1;
            } else if (Math.abs(p.x - 0.5f) < threshold) { //right
                s = 0.5f + (0.5f - p.z) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
                normal.x = 1;
            } else if (Math.abs(p.x + 0.5f) < threshold) { //left
                s = 0.0f + (0.5f - p.z) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
                normal.x = -1;
            } else if (Math.abs(p.y - 0.5f) < threshold) { //top
                s = 0.25f + (0.5f - p.x) * 0.25f;
                t = 0.25f + (0.5f + p.z) * 0.25f;
                normal.y = 1;
            } else if (Math.abs(p.y + 0.5f) < threshold) { //bottom
                s = 0.25f + (0.5f - p.x) * 0.25f;
                t = 0.75f + (0.5f + p.z) * 0.25f;
                normal.y = -1;
            }

            Vector2f textureCoordinates = new Vector2f(s, t);

            Matrix4f normalmatrix = new Matrix4f(modelView.peek());
            normalmatrix.invert().transpose();
            normal = normalmatrix.transform(normal);
            normal = new Vector4f(new Vector3f(normal.x, normal.y, normal.z).normalize(), 0.0f);
            Matrix4f transformation = new Matrix4f(modelView.peek());
            Vector4f position = transformation.transform(p);

            hr = new HitRecord(tMax, position, this.material, normal, textureCoordinates);
        }
        return hr;
    }
}
