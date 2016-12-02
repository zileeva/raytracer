package sgraph;

import com.jogamp.nativewindow.util.SurfaceSize;
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

    protected IShape shape;
    /**
     * The material associated with the object instance at this leaf
     */
    protected util.Material material;

    protected String textureName;

    public LeafNode(String instanceOf, IScenegraph graph, String name)
    {
        super(graph,name);
        this.objInstanceName = instanceOf;

        if (this.objInstanceName.contains("box") || this.objInstanceName.contains("cube")) {
            this.shape = new Box();
        } else if (this.objInstanceName.contains("sphere")) {
            this.shape = new Sphere();
        }
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

    /**
     * Finds intersection of the ray and object if it exists
     * @param ray
     * @param modelView
     * @return
     */
    @Override
    public HitRecord intersect(Ray ray, Stack<Matrix4f> modelView) {

        Vector4f start = new Vector4f(ray.getStart());
        Vector4f direction = new Vector4f(ray.getDirection());

        Matrix4f raymatrix = new Matrix4f(modelView.peek());
        raymatrix.invert();

        start = raymatrix.transform(start);
        direction = raymatrix.transform(direction);

        Ray rayInView = new Ray(start, direction);

        this.shape.setMaterial(this.getMaterial());
        HitRecord hr = this.shape.intersect(rayInView, modelView);
        hr.setTextureName(this.textureName);

        return hr;
    }
}
