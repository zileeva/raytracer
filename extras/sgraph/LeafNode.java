package sgraph;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import util.Light;
import util.Ray;
import util.HitRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This node represents the leaf of a scene graph. It is the only type of node that has
 * actual geometry to render.
 * @author Amit Shesh
 */
public class LeafNode extends AbstractNode
{
    /**
     * The name of the object instance that this leaf contains. All object instances are stored
     * in the scene graph itself, so that an instance can be reused in several leaves
     */
    protected String objInstanceName;
    /**
     * The material associated with the object instance at this leaf
     */
    protected util.Material material;

    protected String textureName;

    public LeafNode(String instanceOf,IScenegraph graph,String name)
    {
        super(graph,name);
        this.objInstanceName = instanceOf;
    }



    /*
	 *Set the material of each vertex in this object
	 */
    @Override
    public void setMaterial(util.Material mat) {
        material = new util.Material(mat);
    }

    /**
     * Set texture ID of the texture to be used for this leaf
     * @param name
     */
    @Override
    public void setTextureName(String name)
    {
        textureName = name;
    }

    /*
     * gets the material
     */
    public util.Material getMaterial()
    {
        return material;
    }

    @Override
    public INode clone()
    {
        LeafNode newclone = new LeafNode(this.objInstanceName,scenegraph,name);
        newclone.setMaterial(this.getMaterial());
        return newclone;
    }

    /**
     * Gets all light of this node in the view coordinate system
     * @param modelView
     * @return list of lights
     */
    @Override
    public List<Light> getLights(Stack<Matrix4f> modelView) {
        List<Light> transformLights = this.getNodeLights(modelView);
        return transformLights;
    }



    /**
     * Delegates to the scene graph for rendering. This has two advantages:
     * <ul>
     *     <li>It keeps the leaf light.</li>
     *     <li>It abstracts the actual drawing to the specific implementation of the scene graph renderer</li>
     * </ul>
     * @param context the generic renderer context {@link sgraph.IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     * @throws IllegalArgumentException
     */
    @Override
    public void draw(IScenegraphRenderer context,Stack<Matrix4f> modelView) throws IllegalArgumentException
    {
        if (objInstanceName.length()>0) {
            if (textureName == null || textureName == "") textureName = "white";
            context.drawMesh(objInstanceName,material,textureName,modelView.peek());
        }
    }

    @Override
    public Matrix4f getAnimationTransform() {
        throw new IllegalArgumentException(getName() + " is not a transform node");
    }

    private HitRecord intersectBox(Ray ray, Stack<Matrix4f> modelView) {

        Vector4f start = new Vector4f(ray.getStart());
        Vector4f direction = new Vector4f(ray.getDirection());
        HitRecord hr = new HitRecord();

//        System.out.println(direction);
        float tx_1 = (-0.5f - start.x) / direction.x;
        float ty_1 = (-0.5f - start.y) / direction.y;
        float tz_1 = (-0.5f - start.z) / direction.z;

        float tx_2 = (0.5f - start.x) / direction.x;
        float ty_2 = (0.5f - start.y) / direction.y;
        float tz_2 = (0.5f - start.z) / direction.z;

        float tMin_x = Math.min(tx_1, tx_2);
        float tMin_y = Math.min(ty_1, ty_2);
        float tMin_z = Math.min(tz_1, tz_2);

        float tMax_x = Math.max(tx_1, tx_2);
        float tMax_y = Math.max(ty_1, ty_2);
        float tMax_z = Math.max(tz_1, tz_2);

//        float tMax = Math.max(Math.max(tMin_x, tMin_y), tMin_z); // near
//        float tMin = Math.min(Math.min(tMax_x, tMax_y), tMax_z); // far

        float tMax = Math.max( Math.max( Math.min(tx_1, tx_2), Math.min(ty_1, ty_2)), Math.min(tz_1, tz_2) );
        float tMin = Math.min( Math.min( Math.max(tx_1, tx_2), Math.max(ty_1, ty_2)), Math.max(tz_1, tz_2) );

        if ((tMin >= 0) && (tMax < tMin)) {

//            if (direction.y == 0) {
//                if (start.y >= -0.5f && start.y <= 0.5f) {
//                    tMax = Float.MIN_VALUE;
//                }
//            }
//
//            if (direction.x == 0) {
//                if (start.x >= -0.5f && start.x <= 0.5f) {
//                    tMax = Float.MIN_VALUE;
//                }
//            }
//
//            if (direction.z == 0) {
//                if (start.z >= -0.5f && start.z <= 0.5f) {
//                    tMax = Float.MIN_VALUE;
//                }
//            }

            Vector4f p = new Vector4f(start.add(direction.mul(tMax)));

            // Might be vertically flipped
            float s = 0, t = 0;
            if (p.z == 0.5f) { //front
                s = 0.75f + (0.5f - p.x) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
            } else if (p.z == -0.5f) { //back
                s = 0.25f + (0.5f - p.x) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
            } else if (p.x == 0.5f) { //right
                s = 0.5f + (0.5f - p.z) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
            } else if (p.x == -0.5f) { //left
                s = 0.0f + (0.5f - p.z) * 0.25f;
                t = 0.5f + (0.5f - p.y) * 0.25f;
            } else if (p.y == 0.5f) { //top
                s = 0.25f + (0.5f - p.x) * 0.25f;
                t = 0.25f + (0.5f + p.z) * 0.25f;
            } else if (p.y == -0.5f) { //bottom
                s = 0.25f + (0.5f - p.x) * 0.25f;
                t = 0.75f + (0.5f + p.z) * 0.25f;
            }
            Vector2f textureCoordinates = new Vector2f(s, t);

//            if (Math.abs(p.x) == 1) {
//                s = (p.z + 1) / 2;
//                t = (p.y + 1) / 2;
//            } else if (Math.abs(p.y) == 1) {
//                s = (p.x + 1) / 2;
//                t = (p.z + 1) / 2;
//            } else {
//                s = (p.x + 1) / 2;
//                t = (p.y + 1) / 2;
//            }




            Matrix4f normalmatrix = new Matrix4f(modelView.peek());
            normalmatrix = normalmatrix.invert().transpose();
            p = normalmatrix.transform(p);
            Vector4f normal = new Vector4f(p.x, p.y, p.z, 0);





//            Vector4f normalVector = normalmatrix.transform(normal); //normal.mul(normalmatrix);
//            normal = new Vector4f(normalVector.x, normalVector.y, normalVector.z, 0);


//            if ()

//            System.out.println(p);
            hr = new HitRecord(tMax, p, this.getMaterial(), normal, textureCoordinates);
        }
        return hr;
    }

    private HitRecord intersectSphere(Ray ray, Stack<Matrix4f> modelView) {
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
            Matrix4f normalmatrix = new Matrix4f(modelView.peek());
            normalmatrix = normalmatrix.invert().transpose();

            Vector4f p = new Vector4f(start.add(direction.mul(tMin)));

            float phi = (float) Math.asin(p.y);
            float theta = (float) java.lang.Math.atan(p.z / p.x);
            float t = (phi + (float) (Math.PI / 2) ) / (float) Math.PI;
            float s = theta / (float) (2 * Math.PI);
            Vector2f textureCoordinates = new Vector2f(s, t);


            p = normalmatrix.transform(p);
            Vector4f normal = new Vector4f(p.x, p.y, p.z, 0);
//            normal = normalmatrix.transform(normal);//p.mul(normalmatrix);
            normal = normal.normalize();
//            Vector4f position = p;//normalmatrix.transform(p);




            hr = new HitRecord(tMin, p, this.getMaterial(), normal, textureCoordinates);
        }

        return hr;
    }


    @Override
    public HitRecord intersect(Ray ray, Stack<Matrix4f> modelView) {

        HitRecord hr = new HitRecord();

        Vector4f start = new Vector4f(ray.getStart());
        Vector4f direction = new Vector4f(ray.getDirection());

        Matrix4f raymatrix = new Matrix4f(modelView.peek());
        raymatrix.invert();

        start = raymatrix.transform(start);// start.mul(raymatrix);
        direction = raymatrix.transform(direction);//direction.mul(raymatrix);
        direction = direction.normalize();

        Ray rayInView = new Ray(start, direction);

        if (this.objInstanceName.contains("box") || this.objInstanceName.contains("cube")) {
            hr = this.intersectBox(rayInView, modelView);
        } else if (this.objInstanceName.contains("sphere")) {
            hr = this.intersectSphere(rayInView, modelView);
        }

        hr.setTextureName(this.textureName);

//        hr.addLights(this.getLights(modelView));

        return hr;
    }
}
