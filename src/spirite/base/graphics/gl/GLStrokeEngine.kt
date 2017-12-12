package spirite.base.graphics.gl

import com.jogamp.opengl.GL2
import com.jogamp.opengl.GL3
import spirite.base.brains.tools.ToolSchemes
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption
import spirite.base.graphics.gl.GLEngine.ProgramType
import spirite.base.graphics.gl.GLGeom.Primitive
import spirite.base.pen.DrawPoints
import spirite.base.pen.StrokeEngine
import spirite.base.util.Colors
import spirite.base.util.glu.GLC
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.hybrid.HybridHelper
import spirite.pc.PCUtil
import spirite.pc.graphics.ImageBI
import spirite.pc.graphics.awt.AWTContext

/**
 * The GLStrokeEngine is a StrokeEngine that uses OpenGL to create a particular
 * kind of Stroke.
 *
 * @author Rory Burks
 */
internal class GLStrokeEngine : StrokeEngine() {
    private val engine = GLEngine.getInstance()
    private var fixedLayer: GLImage? = null
    private var displayLayer: GLImage? = null
    private var w: Int = 0
    private var h: Int = 0
    private var trans: Transform? = null

    override fun onStart(trans: Transform, w: Int, h: Int) {
        this.w = w
        this.h = h
        this.trans = trans
        try {
            fixedLayer = GLImage(w, h)
            displayLayer = GLImage(w, h)
        } catch (e: InvalidImageDimensionsExeption) {
        }

    }

    override fun onEnd() {
        if (fixedLayer != null)
            fixedLayer!!.flush()
        if (displayLayer != null)
            displayLayer!!.flush()
        fixedLayer = null
        displayLayer = null
    }


    override fun prepareDisplayLayer() {
        engine.gL2    // Makes sure GL is loaded (really no reason why it shouldn't be, though)

        val glgc = displayLayer!!.graphics
        glgc.clear()

        val params = GLParameters(w, h)
        params.texture = fixedLayer
        glgc.applyPassProgram(ProgramType.PASS_BASIC, params, null)
    }

    override fun drawDisplayLayer(gc: GraphicsContext) {
        if (displayLayer == null) return
        if (gc is AWTContext) {
            try {
                val img = GLImage(w, h)
                val glgc = img.graphics

                glgc.clear()
                val params = GLParameters(w, h)
                params.texture = displayLayer
                glgc.applyPassProgram(ProgramType.PASS_BASIC, params, null)

                val bi = PCUtil.glSurfaceToImage(
                        HybridHelper.BI_FORMAT, engine.width, engine.height)
                gc.drawImage(ImageBI(bi), 0, 0)
                engine.target = 0
            } catch (e: InvalidImageDimensionsExeption) {
            }

        } else if (gc is GLGraphics) {
//			glgc.reset();

            val params = GLParameters(gc.width, gc.height)
            params.texture = displayLayer
            params.flip = gc.isFlip
            params.addParam(GLParameters.GLParam1i("uComp", 0))
            params.addParam(GLParameters.GLParam1f("uAlpha", gc.alpha))

            GLGraphics.setCompositeBlend(params, gc.composite)

            when (this.strokeParams?.mode) {
                ToolSchemes.PenDrawMode.KEEP_ALPHA -> params.setBlendModeExt(
                        GLC.GL_DST_ALPHA, GLC.GL_ONE_MINUS_SRC_ALPHA, GLC.GL_FUNC_ADD,
                        GLC.GL_ZERO, GLC.GL_ONE, GLC.GL_FUNC_ADD)
                ToolSchemes.PenDrawMode.BEHIND -> params.setBlendMode(
                        GLC.GL_ONE_MINUS_DST_ALPHA, GLC.GL_ONE, GLC.GL_FUNC_ADD)
                ToolSchemes.PenDrawMode.NORMAL -> {}
            }


            gc.applyPassProgram(ProgramType.PASS_RENDER, params, gc.transform,
                    0f, 0f, w.toFloat(), h.toFloat())
        }
    }


    override fun drawToLayer(states: DrawPoints, permanent: Boolean): Boolean {
        if (states.length <= 0)
            return false

        val drawTo = if (permanent) fixedLayer else displayLayer

        engine.setTarget(drawTo)
        _stroke(composeVBuffer(states), if (strokeParams!!.hard) 1 else 0)

        if (strokeParams!!.method === StrokeEngine.Method.BASIC) {
            val params = GLParameters(w, h)
            params.texture = drawTo

            engine.applyPassProgram(ProgramType.STROKE_AFTERPASS_INTENSIFY, params, Transform.IdentityMatrix,
                    0f, 0f, w.toFloat(), h.toFloat())
        }

        engine.target = 0

        return true
    }

    //	private void _strokeSpore(PenState ps, BuiltMediumData built) {
    //		float[] raw = new float[4*13];
    //		float x = ps.x;
    //		float y = ps.y;
    //
    //		float size = stroke.getDynamics().getSize(ps) * stroke.getWidth();
    //
    //		Vec2 xy = built.convert(new Vec2(x,y));
    //		raw[0] = xy.x;
    //		raw[1] = xy.y;
    //		raw[2] = size;
    //		raw[3] = ps.pressure;
    //
    //		for( int i=0; i<4; ++i) {
    //			int off = (i+1)*4;
    //			raw[off+0] = xy.x + size/2.0f * (float)Math.cos(Math.PI/2.0*i);
    //			raw[off+1] = xy.y + size/2.0f * (float)Math.sin(Math.PI/2.0*i);
    //			raw[off+2] = stroke.getDynamics().getSize(ps);
    //			raw[off+3] = ps.pressure;
    //		}
    //		for( int i=0; i<8; ++i) {
    //			int off = (i+5)*4;
    //			raw[off+0] = xy.x + size * (float)Math.cos(Math.PI/8.0+Math.PI/4.0*i);
    //			raw[off+1] = xy.y + size * (float)Math.sin(Math.PI/8.0+Math.PI/4.0*i);
    //			raw[off+2] = stroke.getDynamics().getSize(ps);
    //			raw[off+3] = ps.pressure;
    //		}
    //
    //
    //		int w = built.getWidth();
    //		int h = built.getHeight();
    //
    //		GL2 gl = engine.getGL2();
    //		PreparedData pd = engine.prepareRawData(raw, new int[]{2,1,1});
    //
    //		// Clear Surface
    //        int prog = engine.getProgram(ProgramType.STROKE_SPORE);
    //        gl.glUseProgram( prog);
    //
    //        // Bind Attribute Streams
    //        pd.init();
    //
    //        // Bind Uniforms
    //        int u_perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
    //        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
    //        	MatrixBuilder.orthagonalProjectionMatrix(-0.5f, w-0.5f, -0.5f, h-0.5f, -1, 1)
    //        );
    //        gl.glUniformMatrix4fv(u_perspectiveMatrix, 1, true, orthagonalMatrix);
    //        int uColor = gl.glGetUniformLocation( prog, "uColor");
    //        int c = stroke.getColor();
    //        gl.glUniform3f(uColor,
    //        		Colors.getRed(c)/255.0f,
    //        		Colors.getGreen(c)/255.0f,
    //        		Colors.getBlue(c)/255.0f);
    //
    //
    //        gl.glEnable(GL2.GL_MULTISAMPLE);
    //        gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE );
    //        gl.glEnable(GL.GL_BLEND);
    //        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    //        gl.glBlendEquation(GL2.GL_MAX);
    //
    //    	gl.glDrawArrays(GL3.GL_POINTS, 0, 13);
    //
    //
    //
    //        gl.glDisable( GL.GL_BLEND);
    //        gl.glDisable(GL2.GL_MULTISAMPLE);
    //
    //        gl.glUseProgram(0);
    //
    //        pd.deinit();
    //        pd.free();
    //	}

    private inner class GLVBuffer {
        internal var vBuffer: FloatArray? = null
        internal var len: Int = 0
    }

    private fun composeVBuffer(states: DrawPoints): GLVBuffer {

        val vb = GLVBuffer()

        // Step 1: Determine how much space is needed
        val num = states.length + 2

        val raw = FloatArray(BASIC_STRIDE * num)
        var o = 1    // first point is 0,0,0,0
        for (i in 0 until states.length) {
            val off = o++ * BASIC_STRIDE

            // x y z w
            val xy = trans!!.apply(Vec2(states.x[i], states.y[i]))
            raw[off + 0] = xy.x
            raw[off + 1] = xy.y

            // size pressure
            raw[off + 2] = states.w[i] * strokeParams!!.width
            //			raw[off+3] = ps.pressure;

            /*			if( i == states.size()-1 && stroke.getMethod() == Method.PIXEL) {
				// TODO: Exagerate last line segment so pixel drawing works as expected
				raw[off+0] = data.convertX(ps.x)+0.5f;
				raw[off+1] = data.convertY(ps.y)+0.5f;
			}*/
        }

        raw[0] = raw[BASIC_STRIDE]
        raw[1] = raw[BASIC_STRIDE + 1]
        raw[(1 + states.length) * BASIC_STRIDE] = raw[states.length * BASIC_STRIDE]
        raw[(1 + states.length) * BASIC_STRIDE + 1] = raw[states.length * BASIC_STRIDE + 1]

        vb.vBuffer = raw
        vb.len = num

        return vb
    }

    /**
     * Hardware Accelerated Strokes work by feeding a linestrip with position,
     * size, pressure, and color information.  The vertex shader usually just
     * passes it to a geometry shader that will expand it into a proper shape
     * to be filled by the fragment shader.
     */
    private fun _stroke(glvb: GLVBuffer, mode: Int) {
        val params = GLParameters(w, h)

        val c = this.strokeParams!!.color
        params.addParam(GLParameters.GLParam3f("uColor",
                Colors.getRed(c) / 255.0f,
                Colors.getGreen(c) / 255.0f,
                Colors.getBlue(c) / 255.0f))
        params.addParam(GLParameters.GLParam1i("uMode", mode))
        params.addParam(GLParameters.GLParam1f("uH", h.toFloat()))
        params.setBlendMode(GLC.GL_SRC_ALPHA, GLC.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_MAX)

        if (HybridHelper.getGLCore().shaderVersion >= 330) {
            params.addParam(GLParameters.GLParam1f("uH", h.toFloat()))

            if (this.strokeParams!!.method === StrokeEngine.Method.PIXEL) {
                engine.applyPrimitiveProgram(ProgramType.STROKE_PIXEL, params, Primitive(
                        glvb.vBuffer, intArrayOf(2, 1), GL3.GL_LINE_STRIP_ADJACENCY, intArrayOf(glvb.len)))
            } else {
                val prims = GLGeom.strokeV2LinePassGeom(glvb.vBuffer!!)

                //				strokeParams.texture = new ;
                params.addParam(GLParameters.GLParam1f("uAlpha", 1f))
                engine.applyPrimitiveProgram(ProgramType.POLY_RENDER, params, prims[1])
                engine.applyPrimitiveProgram(if (this.strokeParams!!.hard) ProgramType.STROKE_PIXEL else ProgramType.STROKE_V2_LINE_PASS, params, prims[0])
            }
        } else {
            val type = if (this.strokeParams!!.method === StrokeEngine.Method.PIXEL)
                ProgramType.STROKE_PIXEL
            else
                ProgramType.STROKE_BASIC
            engine.applyPrimitiveProgram(type, params, GLGeom.strokeBasicGeom(glvb.vBuffer!!, h.toFloat()))
        }
    }

    companion object {
        private val BASIC_STRIDE = 3
    }
}
