import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;


import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
    private enum TypeOfCamera {GLOBAL,FPS};
    private int WINDOW_WIDTH,WINDOW_HEIGHT;
    private Stack<Matrix4f> modelView;
    private Matrix4f projection,trackballTransform, keyboardTransform;
    private float trackballRadius, angleOfRotation;
    private Vector2f mousePos;


    private util.ShaderProgram program;
    private util.ShaderLocationsVault shaderLocations;
    private int projectionLocation;
    private sgraph.IScenegraph<VertexAttrib> scenegraph;

    private float time = 0;

    TypeOfCamera cameraMode;

    private Boolean raytrace;


    public View()
    {
        projection = new Matrix4f();
        modelView = new Stack<Matrix4f>();
        trackballRadius = 300;
        trackballTransform = new Matrix4f();
        keyboardTransform = new Matrix4f();
        scenegraph = null;
        angleOfRotation = 1;
        cameraMode = TypeOfCamera.GLOBAL;
        raytrace = false;
    }

    public void initScenegraph(GLAutoDrawable gla,InputStream in) throws Exception
    {
        GL3 gl = gla.getGL().getGL3();

        if (scenegraph!=null)
            scenegraph.dispose();

        program.enable(gl);

        scenegraph = sgraph.SceneXMLReader.importScenegraph(in,new VertexAttribProducer());
        System.out.println(scenegraph.getNodes());

        sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
        renderer.setContext(gla);
        Map<String,String> shaderVarsToVertexAttribs = new HashMap<String,String>();
        shaderVarsToVertexAttribs.put("vPosition","position");
        shaderVarsToVertexAttribs.put("vNormal","normal");
        shaderVarsToVertexAttribs.put("vTexCoord","texcoord");
        renderer.initShaderProgram(program,shaderVarsToVertexAttribs);
        scenegraph.setRenderer(renderer);
        program.disable(gl);
    }

    public void init(GLAutoDrawable gla) throws Exception {
        GL3 gl = gla.getGL().getGL3();

        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new util.ShaderProgram();

        program.createProgram(gl, "shaders/phong-multiple.vert", "shaders/phong-multiple.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        //get input variables that need to be given to the shader program
        projectionLocation = shaderLocations.getLocation("projection");
    }



    public void draw(GLAutoDrawable gla) {
        while (!modelView.empty())
            modelView.pop();

        modelView.push(new Matrix4f());

//        modelView.peek().lookAt(new Vector3f(70, 50, 50), new Vector3f(0,0,0), new Vector3f(0,1,0)).mul(trackballTransform);
        modelView.peek().lookAt(new Vector3f(70, 100, - 80), new Vector3f(0,0,0), new Vector3f(0,1,0)).mul(trackballTransform);

        if (raytrace) {
            scenegraph.raytrace(WINDOW_WIDTH, WINDOW_HEIGHT, modelView);
            raytrace = false;
        } else {
            drawOpenGL(gla);
        }
    }

    public void drawOpenGL(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);
//        gl.glClearColor(0.0f, 0.0f , 0.0f, 1);

        gl.glClearColor(0.69f, 0.8f , 0.9f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);

        program.enable(gl);

//        modelView.peek().mul(trackballTransform);

//        if (cameraMode == TypeOfCamera.GLOBAL) {
//            modelView.peek()
//                    .mul(keyboardTransform)
//                    .lookAt(new Vector3f(0,400,600),new Vector3f(0,0,0),new Vector3f(0,1,0))
//                    .mul(trackballTransform);
//        } else {
//            modelView.peek()
////                    .rotate(45, 0.0f, 0.0f, 1.0f)
//                    .mul(keyboardTransform)
//                    .rotate( (float) Math.toRadians(90), 0.0f, 1.0f, 0.0f)
//                    .translate(0, -100, -125)
//
//                    .mul(new Matrix4f(scenegraph.getAnimationTransform()).invert());
//        }

        gl.glUniformMatrix4fv(projectionLocation,1,false,projection.get(fb16));


        //gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        scenegraph.draw(modelView);
//        scenegraph.animate(time);

        time = time + 1;
        gl.glFlush();

        program.disable(gl);



    }

    public void setRaytrace() {
        raytrace = true;
    }

    public void shift(String d) {
        switch (d) {
            case "up":
                keyboardTransform = new Matrix4f()
                        .translate(0, 2f * -angleOfRotation, 0)
                        .mul(keyboardTransform);
                break;
            case "down":
                keyboardTransform = new Matrix4f()
                        .translate(0, 2f * angleOfRotation, 0)
                        .mul(keyboardTransform);
                break;
            case "left":
                keyboardTransform = new Matrix4f()
                        .translate(2f * angleOfRotation, 0, 0)
                        .mul(keyboardTransform);
                break;
            case "right":
                keyboardTransform = new Matrix4f()
                        .translate(2f * -angleOfRotation, 0, 0)
                        .mul(keyboardTransform);
                break;
            default:
                break;
        }
    }

    public void nod(String d) {
        switch (d) {
            case "up":
                keyboardTransform = new Matrix4f()
                        .rotate(0.05f * -angleOfRotation, 1, 0, 0)
                        .mul(keyboardTransform);
                break;
            case "down":
                keyboardTransform = new Matrix4f()
                        .rotate(0.05f * angleOfRotation, 1, 0, 0)
                        .mul(keyboardTransform);
                break;
            case "left":
                keyboardTransform = new Matrix4f()
                        .rotate(0.05f * -angleOfRotation, 0, 1, 0)
                        .mul(keyboardTransform);
                break;
            case "right":
                keyboardTransform = new Matrix4f()
                        .rotate(0.05f * angleOfRotation, 0, 1, 0)
                        .mul(keyboardTransform);
                break;
            case "cc":
                keyboardTransform = new Matrix4f()
                        .rotate(0.05f * angleOfRotation, 0, 0, 1)
                        .mul(keyboardTransform);
                break;
            case "c":
                keyboardTransform = new Matrix4f()
                        .rotate(0.05f * -angleOfRotation, 0, 0, 1)
                        .mul(keyboardTransform);
                break;
            default:
                break;
        }
    }

    public void setFPS()
    {
        cameraMode = TypeOfCamera.FPS;
    }

    public void setGlobal()
    {
        cameraMode = TypeOfCamera.GLOBAL;
    }

    public void mousePressed(int x,int y)
    {
        mousePos = new Vector2f(x,y);
    }

    public void mouseReleased(int x,int y)
    {
        System.out.println("Released");
    }

    public void mouseDragged(int x,int y)
    {
        Vector2f newM = new Vector2f(x,y);

        Vector2f delta = new Vector2f(newM.x-mousePos.x,newM.y-mousePos.y);
        mousePos = new Vector2f(newM);

        trackballTransform = new Matrix4f().rotate(delta.x/trackballRadius,0,1,0)
                                           .rotate(delta.y/trackballRadius,1,0,0)
                                           .mul(trackballTransform);
    }

    public void reshape(GLAutoDrawable gla,int x,int y,int width,int height)
    {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        projection = new Matrix4f().perspective((float)Math.toRadians(120.0f),(float)width / height,0.1f,10000.0f);
//        projection = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

    }

    public void dispose(GLAutoDrawable gla)
    {
        GL3 gl = gla.getGL().getGL3();

    }



}
