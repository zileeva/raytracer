package sgraph;

import com.jogamp.opengl.GL3;
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

        Vector4f start = ray.getStart();
        Vector4f direction = ray.getDirection();
        HitRecord hr = new HitRecord();

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

        float tMax = Math.max(Math.max(tMin_x, tMin_y), tMin_z); // near
        float tMin = Math.min(Math.min(tMax_x, tMax_y), tMax_z); // far

        if ((tMax > 0.0f) && (tMax < tMin)) {
            Vector4f p = start.add(direction.mul(tMax));
            Vector4f normal = new Vector4f(0, 0, 0, 0);
//            System.out.println(p);
            hr = new HitRecord(tMax, p, this.getMaterial(), normal);
        }
        return hr;
    }

    private HitRecord intersectSphere(Ray ray, Stack<Matrix4f> modelView) {
        Vector4f start = ray.getStart();
        Vector4f direction = ray.getDirection();
        HitRecord hr = new HitRecord();

        float A = direction.x * direction.x + direction.y * direction.y + direction.z * direction.z;
        float B = 2 * (direction.x * start.x + direction.y * start.y + direction.z * start.z);
        float C = start.x * start.x + start.y * start.y + start.z * start.z - 1;

        float b24ac = B * B - 4 * A * C;

        float t1 = (-B + (float) Math.sqrt(b24ac)) / (2 * A);
        float t2 = (-B - (float) Math.sqrt(b24ac)) / (2 * A);

        float tMin = Math.min(t1, t2);

        if (b24ac > 0 && tMin > 0) {
            Vector4f p = start.add(direction.mul(tMin));
            Vector4f normal = p.sub(new Vector4f(0, 0, 0, 0));
//            System.out.println(p);
            hr = new HitRecord(tMin, p, this.getMaterial(), normal);
        }

        return hr;
    }


    @Override
    public HitRecord intersect(Ray ray, Stack<Matrix4f> modelView) {

        HitRecord hr = new HitRecord();

        if (this.objInstanceName.contains("box") || this.objInstanceName.contains("cube")) {
            hr = this.intersectBox(ray, modelView);
        } else if (this.objInstanceName.contains("sphere")) {
            hr = this.intersectSphere(ray, modelView);
        }

//        HitRecord hr = new HitRecord(new Vector2f(tMax, tMin));
        return hr;
    }
}
