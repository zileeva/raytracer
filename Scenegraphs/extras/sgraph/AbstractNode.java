package sgraph;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import util.Light;
import java.util.*;

import java.util.ArrayList;

/**
 * This abstract class implements the {@link sgraph.INode} interface. It provides default methods
 * for many of the methods, especially the ones that could throw an exception
 * Child classes that do not want these exceptions throws should override these methods
 * @author Amit Shesh
 */
public abstract class AbstractNode implements INode
{
    /**
     * The name given to this node
     */
    protected String name;
    /**
     * The parent of this node. Each node except the root has a parent. The root's parent is null
     */
    protected INode parent;
    /**
     * A reference to the {@link sgraph.IScenegraph} object that this is part of
     */
    protected IScenegraph scenegraph;

    protected List<util.Light> lights = new ArrayList<>();

    public AbstractNode(IScenegraph graph,String name)
    {
        this.parent = null;
        scenegraph = graph;
        setName(name);
    }

    /**
     * By default, this method checks only itself. Nodes that have children should override this
     * method and navigate to children to find the one with the correct name
     * @param name name of node to be searched
     * @return the node whose name this is, null otherwise
     */
    public INode getNode(String name)
    {
        if (this.name.equals(name))
        return this;

        return null;
    }

    /**
     * Sets the parent of this node
     * @param parent the node that is to be the parent of this node
     */

    public void setParent(INode parent)
    {
        this.parent = parent;
    }

    /**
     * Sets the scene graph object whose part this node is and then adds itself
     * to the scenegraph (in case the scene graph ever needs to directly access this node)
     * @param graph a reference to the scenegraph object of which this tree is a part
     */
    public void setScenegraph(IScenegraph graph)
    {
        this.scenegraph = graph;
        graph.addNode(this.name,this);
    }

    /**
     * Sets the name of this node
     * @param name the name of this node
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the name of this node
     * @return the name of this node
     */
    public String getName() { return name;}


    public abstract INode clone();

    /**
     * By default, throws an exception. Any nodes that can have children should override this
     * method
     * @param child
     * @throws IllegalArgumentException
     */
    @Override
    public void addChild(INode child) throws IllegalArgumentException
    {
        throw new IllegalArgumentException("Not a composite node");
    }

    /**
     * By default, throws an exception. Any nodes that are capable of storing transformations
     * should override this method
     * @param t
     */

    @Override
    public void setTransform(Matrix4f t)
    {
        throw new IllegalArgumentException(getName()+" is not a transform node");
    }


    /**
     * By default, throws an exception. Any nodes that are capable of storing transformations
     * should override this method
     * @param t
     */

    @Override
    public void setAnimationTransform(Matrix4f t)
    {
        throw new IllegalArgumentException(getName()+" is not a transform node");
    }

    @Override
    public Matrix4f getTransform() {
        throw new IllegalArgumentException(getName()+" is not a transform node");
    }


    /**
     * By default, throws an exception. Any nodes that are capable of storing material should
     * override this method
     * @param m the material object to be associated with this node
     */
    @Override
    public void setMaterial(util.Material m)
    {
        throw new IllegalArgumentException(getName()+" is not a leaf node");
    }

    @Override
    public void setTextureName(String name) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Textures not supported yet!");
    }

    /**
     * Gets all lights from this node in the view coordinate system
     * @param modelView
     * @return
     */
    public List<Light> getNodeLights(Stack<Matrix4f> modelView) {
        List<Light> transformLights = new ArrayList<>();
        for (Light light : this.lights) {
            Vector4f pos = light.getPosition();
            Vector4f spotD = light.getSpotDirection();
            Matrix4f transformation = new Matrix4f(modelView.peek());
            pos = transformation.transform(pos);
            spotD = transformation.transform(spotD);
            Light l = new util.Light();
            l.setAmbient(light.getAmbient());
            l.setDiffuse(light.getDiffuse());
            l.setSpecular(light.getSpecular());
            l.setSpotDirection(spotD.x, spotD.y, spotD.z);
            l.setSpotAngle(light.getSpotCutoff());
            l.setPosition(pos);
            transformLights.add(l);
        }
        return transformLights;
    }

    /**
     * Adds a new light to this node.
     * @param l
     */
    public void addLight(Light l) {
        this.lights.add(l);
    }

}
