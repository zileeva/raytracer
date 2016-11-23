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

    private Vector4f reflect(Vector4f I, Vector4f N) {
        float NdotI = N.dot(I);
        NdotI = 2.0f * NdotI;
        Vector4f r = new Vector4f(
                I.sub(N.mul(NdotI))
        );//I - 2.0 * dot(N, I) * N
        return r;
    }

    private Color shade(HitRecord hitRecord) {

        List<Light> lights = hitRecord.getLights();
        Color color = new Color(255, 255, 255);
        Material material = hitRecord.getMaterial();
        Vector4f position = hitRecord.getP();
        Vector4f normal = hitRecord.getNormal();

        for (int i = 0; i < lights.size(); i ++) {
            Light light = lights.get(i);
            Vector3f ambient, diffuse, specular;
            Vector4f lightVec;
            if (light.getPosition().w != 0) {
                lightVec = new Vector4f(light.getPosition().sub(position)).normalize();
            } else {
                lightVec = light.getPosition().negate();
            }

            Vector4f normalView = normal.normalize();
            float nDotL = normalView.dot(lightVec);
            nDotL = Math.max(nDotL, 0);

            Vector4f viewVec = new Vector4f(position).negate();
            viewVec = viewVec.normalize();

            Vector4f reflectVec = reflect(lightVec.negate(), normalView);
            reflectVec = reflectVec.normalize();
            float rDotV = Math.max(reflectVec.dot(viewVec), 0.0f);

            ambient = new Vector3f(
                    material.getAmbient().x * light.getAmbient().x,
                    material.getAmbient().y * light.getAmbient().y,
                    material.getAmbient().z * light.getAmbient().z);

            diffuse = new Vector3f(
                    material.getDiffuse().x * light.getDiffuse().x * nDotL,
                    material.getDiffuse().y * light.getDiffuse().y * nDotL,
                    material.getDiffuse().z * light.getDiffuse().z * nDotL);

            if (nDotL > 0) {
                specular = new Vector3f(
                        material.getSpecular().x * light.getSpecular().x * (float) Math.pow(rDotV, material.getShininess()),
                        material.getSpecular().y * light.getSpecular().y * (float) Math.pow(rDotV, material.getShininess()),
                        material.getSpecular().z * light.getSpecular().z * (float) Math.pow(rDotV, material.getShininess())
                        );
            } else {
                specular = new Vector3f(0, 0, 0);
            }

            float spotAngle = (float) Math.cos(Math.toRadians(light.getSpotCutoff()));
            if ( (new Vector4f(lightVec.negate())).dot(new Vector4f(light.getSpotDirection()).normalize()) > spotAngle) {
                color = new Color(

                        ambient.x + diffuse.x + specular.x,
                        ambient.y + diffuse.y + specular.y,
                        ambient.z + diffuse.z + specular.z);
            }

        }
        return color;
//        vec3 lightVec,viewVec,reflectVec;
//        vec3 normalView;
//        vec3 ambient,diffuse,specular;
//        float nDotL,rDotV;
//
//
//        fColor = vec4(0,0,0,1);
//
//        for (int i=0;i<numLights;i++)
//        {
//            if (light[i].position.w!=0)
//                lightVec = normalize(light[i].position.xyz - fPosition.xyz);
//            else
//                lightVec = normalize(-light[i].position.xyz);
//
//            vec3 tNormal = fNormal;
//            normalView = normalize(tNormal.xyz);
//            nDotL = dot(normalView,lightVec);
//
//            viewVec = -fPosition.xyz;
//            viewVec = normalize(viewVec);
//
//            reflectVec = reflect(-lightVec,normalView);
//            reflectVec = normalize(reflectVec);
//
//            rDotV = max(dot(reflectVec,viewVec),0.0);
//
//            ambient = material.ambient * light[i].ambient;
//            diffuse = material.diffuse * light[i].diffuse * max(nDotL,0);
//            if (nDotL>0)
//                specular = material.specular * light[i].specular * pow(rDotV,material.shininess);
//            else
//                specular = vec3(0,0,0);
//
//            vec3 sd = normalize(light[i].spotDirection.xyz);
//            if ( dot(-lightVec, sd) > light[i].spotAngle) fColor = clamp(fColor +  vec4(ambient + diffuse + specular, 1.0), 0, 1);
//            //if (spotlight > 0.1) fColor = fColor +  vec4(ambient + diffuse + specular, 1.0);
//        }
    }

    /**
     * Determines if this ray hits anything in the scene graph
     * @param ray in the view coordinate system
     * @param modelView
     */
    public Color raycast(Ray ray, Stack<Matrix4f> modelView) {
        HitRecord hr = root.intersect(ray, modelView);
        hr.setLights(root.getLights(modelView));
//        Color color = new Color(0, 0, 0);
        Color color = new Color(0.69f, 0.8f , 0.9f);
        if (hr.isHit()) {
            color = this.shade(hr);
//            r = g = b = 255;
        }

        return color;
    }

    @Override
    public void raytrace(int width, int height, Stack<Matrix4f> modelView) {
        int i, j;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {

                //get color in (r,g,b)
                Vector4f start = new Vector4f(0, 0, 0, 1);
                Vector4f direction = new Vector4f(i - width/2, j - height/2, (-0.5f * height) / (float) Math.tan(Math.toRadians(0.5f * 120.0f)), 0);

//                Matrix4f view = new Matrix4f(modelView.peek());
//                Matrix4f viewInverted = view.invert();
//                start.mul(viewInverted);
//                direction.mul(viewInverted);

                Ray ray = new Ray(start, direction);

                Color color = this.raycast(ray, modelView);

                output.setRGB(i, j, color.getRGB());
            }
        }

        OutputStream outStream = null;

        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -output.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        output = op.filter(output, null);

        try {
            outStream = new FileOutputStream("output/raytrace.png");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }

        try {
            ImageIO.write(output, "png", outStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }
    }



}
