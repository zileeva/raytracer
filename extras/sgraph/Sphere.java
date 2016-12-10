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
public class Sphere implements IShape {

    private Material material;

    public Sphere() {
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

            float A = direction.x * direction.x + direction.y * direction.y + direction.z * direction.z;
            float B = 2 * (direction.x * start.x + direction.y * start.y + direction.z * start.z);
            float C = start.x * start.x + start.y * start.y + start.z * start.z - 1;

            float D = B * B - 4 * A * C;

            float t1 = (-B + (float) Math.sqrt(D)) / (2 * A);
            float t2 = (-B - (float) Math.sqrt(D)) / (2 * A);

            float tMin = Math.min(t1, t2);

            if (D > 0 && tMin > 0) {

                Vector4f p = new Vector4f(start.add(direction.mul(tMin)));

                float theta = (float) java.lang.Math.atan2(p.z, p.x);
                if (theta < 0) theta += 2 * Math.PI;
                float s = theta / (float) (2 * Math.PI);

                float phi = (float) Math.asin(p.y);
                float t =(phi + (float) (Math.PI / 2) ) / (float) Math.PI;

                Vector2f textureCoordinates = new Vector2f(-s, -t);

                Vector4f normal = new Vector4f(p.x, p.y, p.z, 0);
                Matrix4f normalmatrix = new Matrix4f(modelView.peek());
                normalmatrix.invert().transpose();
                normal = normalmatrix.transform(normal);
                normal = new Vector4f(new Vector3f(normal.x, normal.y, normal.z).normalize(), 0.0f);
                Matrix4f transformation = new Matrix4f(modelView.peek());
                Vector4f position = transformation.transform(p);

                hr = new HitRecord(tMin, position, this.material, normal, textureCoordinates);

            }

            return hr;
    }
}
