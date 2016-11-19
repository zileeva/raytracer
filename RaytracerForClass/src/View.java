import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import util.ObjectInstance;
import util.TextureImage;


import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly
 * encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
  private int WINDOW_WIDTH, WINDOW_HEIGHT;
  private Matrix4f proj, modelView;
  private List<ObjectInstance> meshObjects;
  private List<util.TextureImage> textures;
  private List<util.Material> materials;
  private List<Matrix4f> transforms;
  private List<util.Light> lights;
  private Matrix4f trackballTransform;
  private float trackballRadius;
  private Vector2f mousePos;
  private util.ShaderProgram program;
  util.ShaderLocationsVault shaderLocations;
  private boolean raytrace;


  public View() {
    proj = new Matrix4f();
    proj.identity();

    modelView = new Matrix4f();
    modelView.identity();

    meshObjects = new ArrayList<ObjectInstance>();
    transforms = new ArrayList<Matrix4f>();
    materials = new ArrayList<util.Material>();
    lights = new ArrayList<util.Light>();
    textures = new ArrayList<TextureImage>();

    trackballTransform = new Matrix4f();
    trackballRadius = 300;
    raytrace = false;
  }

  public void setRaytrace() {
    raytrace = true;
  }

  private void initObjects(GL3 gl) throws FileNotFoundException, IOException {

    util.PolygonMesh<?> tmesh;

    InputStream in;

    in = getClass().getClassLoader().getResourceAsStream("models/box-outside.obj");

    tmesh = util.ObjImporter.importFile(new VertexAttribProducer(),
            in, true);

    util.ObjectInstance obj;

    Map<String, String> shaderToVertexAttribute = new HashMap<String, String>();

    shaderToVertexAttribute.put("vPosition", "position");
    shaderToVertexAttribute.put("vNormal", "normal");
    shaderToVertexAttribute.put("vTexCoord", "texcoord");


    obj = new util.ObjectInstance(
            gl,
            program,
            shaderLocations,
            shaderToVertexAttribute,
            tmesh, new String(""));
    meshObjects.add(obj);
    util.Material mat;

    mat = new util.Material();

    mat.setAmbient(0.5f, 0.5f, 0.5f);
    mat.setDiffuse(0.6f, 0.6f, 0.6f);
    mat.setSpecular(0.6f, 0.6f, 0.6f);
    mat.setShininess(100);
    materials.add(mat);

    Matrix4f t;

    t = new Matrix4f();
    transforms.add(t);

    // textures

    util.TextureImage textureImage;

    textureImage = new util.TextureImage("textures/die.png",
            "png",
            "white");

    Texture tex = textureImage.getTexture();


    tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
    tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
    tex.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
    tex.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

    textures.add(textureImage);
  }

  private void initLights() {
    util.Light l = new util.Light();
    l.setAmbient(0.5f, 0.5f, 0.5f);
    l.setDiffuse(0.5f, 0.5f, 0.5f);
    l.setSpecular(0.5f, 0.5f, 0.5f);
    l.setPosition(-100, 100, 100);
    lights.add(l);

    l = new util.Light();
    l.setAmbient(0.5f, 0.5f, 0.5f);
    l.setDiffuse(0.5f, 0.5f, 0.5f);
    l.setSpecular(0.5f, 0.5f, 0.5f);
    l.setPosition(100, 100, 100);
    lights.add(l);

  }


  public void init(GLAutoDrawable gla) throws Exception {
    GL3 gl = gla.getGL().getGL3();


    //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
    program = new util.ShaderProgram();
    program.createProgram(gl, "shaders/lights-textures.vert", "shaders/lights-textures.frag");

    shaderLocations = program.getAllShaderVariables(gl);
    initObjects(gl);
    initLights();

  }


  private void raytrace(int width, int height) {
    int i, j;

    BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    for (i = 0; i < width; i++) {
      for (j = 0; j < height; j++) {
                /*
                 create ray in view coordinates
                 start point: 0,0,0 always!
                 going through near plane pixel (i,j)
                 So 3D location of that pixel in view coordinates is
                 x = i-width/2
                 y = j-height/2
                 z = -0.5*height/tan(0.5*FOVY)
                */

        //get color in (r,g,b)
        Vector4f start = new Vector4f(0, 0, 0, 1);
        Vector4f direction = new Vector4f(i - width/2, j - height/2, (-0.5f * height) / (float) Math.tan(Math.toRadians(0.5f * 120.0f)), 0);

        Matrix4f lookat = new Matrix4f().lookAt(new Vector3f(0, 0, 1.5f), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        start.mul(lookat.invert());
        direction.mul(lookat.invert());

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

        int r, g, b;

        if ((tMax > 0.0f) && (tMax < tMin)) {
          r = g = b = 255;
        } else {
          r = g = b = 0;
        }
//        if ((i + j) % 10 < 5)
//          r = g = b = 0;
//        else
//          r = g = b = 255;
        output.setRGB(i, j, new Color(r, g, b).getRGB());
      }
    }

    OutputStream outStream = null;

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


  public void draw(GLAutoDrawable gla) {
    modelView = new Matrix4f().lookAt(new Vector3f(0, 0, 1.5f), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));

    if (raytrace) {
      raytrace(WINDOW_WIDTH, WINDOW_HEIGHT);
      raytrace = false;
    } else {
      drawOpenGL(gla);
    }
  }

  public void drawOpenGL(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();
    FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
    FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
    gl.glEnable(GL.GL_DEPTH_TEST);

    program.enable(gl);
    for (int i = 0; i < lights.size(); i++) {

      Vector4f pos = lights.get(i).getPosition();
      Matrix4f lightTransformation;

      lightTransformation = new Matrix4f(modelView);
      pos = lightTransformation.transform(pos);
      String varName = "light[" + i + "].position";

      gl.glUniform4fv(shaderLocations.getLocation(varName), 1, pos.get
              (fb4));
    }

    /*
     *Supply the shader with all the matrices it expects.
    */
    gl.glUniformMatrix4fv(
            shaderLocations.getLocation("projection"),
            1,
            false, proj.get(fb16));


    //all the light properties, except positions
    gl.glUniform1i(shaderLocations.getLocation("numLights"),
            lights.size());
    for (int i = 0; i < lights.size(); i++) {
      String name = "light[" + i + "].";
      gl.glUniform3fv(shaderLocations.getLocation(name + "ambient"),
              1, lights.get(i).getAmbient().get(fb4));
      gl.glUniform3fv(shaderLocations.getLocation(name + "diffuse"),
              1, lights.get(i).getDiffuse().get(fb4));
      gl.glUniform3fv(shaderLocations.getLocation(name + "specular"),
              1, lights.get(i).getSpecular().get(fb4));
    }

    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glActiveTexture(GL.GL_TEXTURE0);


    gl.glUniform1i(shaderLocations.getLocation("image"), 0);


    for (int i = 0; i < meshObjects.size(); i++) {
      Matrix4f transformation = new Matrix4f().mul(modelView).mul(trackballTransform).mul(transforms.get(i));
      Matrix4f normalmatrix = new Matrix4f(transformation);
      normalmatrix = normalmatrix.invert().transpose();
      gl.glUniformMatrix4fv(shaderLocations.getLocation("modelview"), 1, false, transformation.get(fb16));
      gl.glUniformMatrix4fv(shaderLocations.getLocation("normalmatrix"), 1, false, normalmatrix.get(fb16));

      gl.glUniform3fv(shaderLocations.getLocation("material.ambient"), 1, materials.get(i).getAmbient().get(fb4));
      gl.glUniform3fv(shaderLocations.getLocation("material.diffuse"), 1, materials.get(i).getDiffuse().get(fb4));
      gl.glUniform3fv(shaderLocations.getLocation("material.specular"), 1, materials.get(i).getSpecular().get(fb4));
      gl.glUniform1f(shaderLocations.getLocation("material.shininess"), materials.get(i).getShininess());

      textures.get(i).getTexture().bind(gl);
      meshObjects.get(i).draw(gla);
    }
    gl.glFlush();

    program.disable(gl);


  }

  public void mousePressed(int x, int y) {
    mousePos = new Vector2f(x, y);
  }

  public void mouseReleased(int x, int y) {
    System.out.println("Released");
  }

  public void mouseDragged(int x, int y) {
    Vector2f newM = new Vector2f(x, y);

    Vector2f delta = new Vector2f(newM.x - mousePos.x, newM.y - mousePos.y);
    mousePos = new Vector2f(newM);

    trackballTransform = new Matrix4f().rotate(delta.x / trackballRadius, 0, 1, 0)
            .rotate(delta.y / trackballRadius, 1, 0, 0)
            .mul(trackballTransform);
  }

  public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
    GL gl = gla.getGL();
    WINDOW_WIDTH = width;
    WINDOW_HEIGHT = height;
    gl.glViewport(0, 0, width, height);

    proj = new Matrix4f().perspective((float) Math.toRadians(120.0f), (float) width / height, 0.1f, 10000.0f);
    //proj = new Matrix4f().ortho(-50,50,-50,50,0.1f,10000.0f);

  }

  public void dispose(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();

  }


}
