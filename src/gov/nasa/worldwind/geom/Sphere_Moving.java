package gov.nasa.worldwind.geom;

import javax.media.opengl.GL;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

public final class Sphere_Moving extends java.util.TimerTask implements Renderable
{
    public final static Sphere_Moving UNIT_SPHERE = new Sphere_Moving(Vec4.ZERO, 1);
    private Vec4 center;
    private double radius; 

    public Sphere_Moving(Vec4 center, double radius)
    {
        if (center == null)
        {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (radius <= 0)
        {
            String message = Logging.getMessage("Geom.Sphere.RadiusIsZeroOrNegative", radius);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.center = center;
        this.radius = radius;
    }

    
    public void run() { //run in interface Runnabl
        this.radius=this.radius+10000;
       
    }

    
    public final double getRadius()
    {
        return this.radius;
    }

    public final double getDiameter()
    {
        return 2 * this.radius;
    }

    public final Vec4 getCenter()
    {
        return this.center;
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        
        javax.media.opengl.GL gl = dc.getGL();
        gl.glPushAttrib(javax.media.opengl.GL.GL_ENABLE_BIT | javax.media.opengl.GL.GL_CURRENT_BIT);
        gl.glMatrixMode(javax.media.opengl.GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glTranslated(this.center.x, this.center.y, this.center.z);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        setLightModel(gl);
        setLightDirection(gl, new Vec4(1.0, 0.5, 1.0));
        
        
     
        
        
        javax.media.opengl.glu.GLUquadric quadric = dc.getGLU().gluNewQuadric();
        dc.getGLU().gluQuadricDrawStyle(quadric, javax.media.opengl.glu.GLU.GLU_FILL);
        dc.getGLU().gluSphere(quadric, this.radius, 10, 10);
        gl.glPopMatrix();
        dc.getGLU().gluDeleteQuadric(quadric);
        gl.glPopAttrib();
    }
    
    
    protected static void setLightModel(GL gl)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        float[] modelAmbient  = new float[4];
        modelAmbient[0] = 1.0f;
        modelAmbient[1] = 1.0f;
        modelAmbient[2] = 1.0f;
        modelAmbient[3] = 0.0f;

        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, modelAmbient, 0);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_FALSE);
    }


    public void setLightDirection(GL gl,Vec4 direction)
    {
        if (direction == null)
        {
            String message = Logging.getMessage("nullValue.DirectionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        
        Vec4 vec = direction.normalize3();
        float[] params = new float[4];
        params[0] = (float) vec.x;
        params[1] = (float) vec.y;
        params[2] = (float) vec.z;
        params[3] = 0.0f;
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, params, 0);
        gl.glPopMatrix();

    }
    
    public String toString()
    {
        return "Sphere: center = " + this.center.toString() + " radius = " + Double.toString(this.radius);
    }

}
