package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import util.HitRecord;
import util.IVertexData;
import util.PolygonMesh;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

import util.*;


/**
 * A specific implementation of this scene graph. This implementation is still independent
 * of the rendering technology (i.e. OpenGL)
 * @author Amit Shesh
 */
public class Scenegraph<VertexType extends IVertexData> implements IScenegraph<VertexType>
{
    /**
     * The root of the scene graph tree
     */
    protected INode root;

    /**
     * A map to store the (name,mesh) pairs. A map is chosen for efficient search
     */
    protected Map<String,util.PolygonMesh<VertexType>> meshes;

    /**
     * A map to store the (name,node) pairs. A map is chosen for efficient search
     */
    protected Map<String,INode> nodes;

    protected Map<String,String> textures;

    /**
     * The associated renderer for this scene graph. This must be set before attempting to
     * render the scene graph
     */
    protected IScenegraphRenderer renderer;

    /**
     * Bird scene graph model
     */
    private Bird birdOne, birdTwo;

    /**
     * Bee scene graph model
     */
    private Bee bee;

    public Scenegraph()
    {
        root = null;
        meshes = new HashMap<String,util.PolygonMesh<VertexType>>();
        nodes = new HashMap<String,INode>();
        textures = new HashMap<String,String>();

    }

    public void dispose()
    {
        renderer.dispose();
    }

    /**
     * Sets the renderer, and then adds all the meshes to the renderer.
     * This function must be called when the scene graph is complete, otherwise not all of its
     * meshes will be known to the renderer
     * @param renderer The {@link IScenegraphRenderer} object that will act as its renderer
     * @throws Exception
     */
    @Override
    public void setRenderer(IScenegraphRenderer renderer) throws Exception {
        this.renderer = renderer;

        //now add all the meshes
        for (String meshName:meshes.keySet()) {
            this.renderer.addMesh(meshName, meshes.get(meshName));
        }

        for (Map.Entry<String, String> t : textures.entrySet()) {
            this.renderer.addTexture(t.getKey(), t.getValue());
        }

    }


    /**
     * Set the root of the scenegraph, and then pass a reference to this scene graph object
     * to all its node. This will enable any node to call functions of its associated scene graph
     * @param root
     */

    @Override
    public void makeScenegraph(INode root)
    {
        this.root = root;
        this.root.setScenegraph(this);

        birdOne = new Bird(nodes, "1");
        birdTwo = new Bird(nodes, "2");

        bee = new Bee(nodes, "3");
    }

    /**
     * Draw this scene graph. It delegates this operation to the renderer
     * @param modelView
     */
    @Override
    public void draw(Stack<Matrix4f> modelView) {

        if ((root!=null) && (renderer!=null))
        {
            renderer.draw(root,modelView);
        }
    }


    @Override
    public void addPolygonMesh(String name, util.PolygonMesh<VertexType> mesh)
    {
        meshes.put(name,mesh);
    }

    @Override
    public void animate(float time) {

        birdOne.animate(time);
        birdTwo.animate(time);

        bee.animate(time);

    }

    @Override
    public void addNode(String name, INode node) {
        nodes.put(name,node);
    }


    @Override
    public INode getRoot() {
        return root;
    }

    @Override
    public Map<String, PolygonMesh<VertexType>> getPolygonMeshes() {
        Map<String,util.PolygonMesh<VertexType>> meshes = new HashMap<String,PolygonMesh<VertexType>>(this.meshes);
        return meshes;
    }

    @Override
    public Map<String, INode> getNodes() {
        Map<String,INode> nodes = new TreeMap<String,INode>();
        nodes.putAll(this.nodes);
        return nodes;
    }

    @Override
    public Matrix4f getAnimationTransform() {
        return birdTwo.getBirdNode().getAnimationTransform();
    }

    @Override
    public Matrix4f getTransform() {
        return birdTwo.getBirdNode().getTransform();
    }

    @Override
    public void addTexture(String name, String path) {
        textures.put(name, path);
    }

    @Override
    public void raytrace(int width, int height, Stack<Matrix4f> modelView) {
        Raytracer raytracer = new Raytracer(root, textures);
        raytracer.raytrace(width, height, modelView);
    }




}
