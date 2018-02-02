package spirite.base.graphics.gl

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption
import spirite.base.util.glu.GLC

class GLImage : RawImage {
    override val width : Int
    override val height: Int
    val engine: GLEngine
    override var isGLOriented: Boolean
    var tex : IGLTexture?
        get
        private set

    // region Constructors
    constructor( width: Int, height: Int, glEngine: GLEngine) {
        if( width <= 0 || height <= 0)
            throw InvalidImageDimensionsExeption("Invalid Image Dimensions")
        this.width = width
        this.height = height
        this.engine = glEngine
        this.isGLOriented = true

        val gl = glEngine.gl

        tex = gl.createTexture() ?: throw GLResourcException("Failed to create Texture")
        gl.bindTexture( GLC.GL_TEXTURE_2D, tex)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MIN_FILTER, GLC.GL_NEAREST)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MAG_FILTER, GLC.GL_NEAREST)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_WRAP_S, GLC.GL_CLAMP_TO_EDGE)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_WRAP_T, GLC.GL_CLAMP_TO_EDGE)
        gl.texImage2D( GLC.GL_TEXTURE_2D, 0, GLC.GL_RGBA8, GLC.GL_RGBA, GLC.GL_UNSIGNED_BYTE,
                gl.createBlankTextureSource(width, height))
    }

    constructor( toCopy: GLImage) {
        width = toCopy.width
        height = toCopy.height
        engine = toCopy.engine
        isGLOriented = true

        val gl = engine.gl

        // Set the GL Target as the other image's texture and copy the data
        engine.target =  toCopy.tex
        tex = gl.createTexture() ?: throw GLResourcException("Failed to create Texture")
        gl.bindTexture(GLC.GL_TEXTURE_2D, tex)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MIN_FILTER, GLC.GL_NEAREST)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MAG_FILTER, GLC.GL_NEAREST)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_WRAP_S, GLC.GL_CLAMP_TO_EDGE)
        gl.texParameteri(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_WRAP_T, GLC.GL_CLAMP_TO_EDGE)
        gl.copyTexImage2D(GLC.GL_TEXTURE_2D, 0, GLC.GL_RGBA8, 0, 0, width, height, 0)
    }

    constructor( tex: IGLTexture, width: Int, height: Int, glEngine: GLEngine, isGLOriented: Boolean) {
        this.tex = tex
        this.width = width
        this.height = height
        this.engine = glEngine
        this.isGLOriented = isGLOriented
    }
    // endregion

    override val graphics: GraphicsContext = GLGraphics(this)
    override val byteSize: Int get() = width*height*4

    override fun flush() {
        val toDel = tex
        if( toDel != null) {
            // Must be run on the AWT Thread to prevent JOGL-internal deadlocks
            engine.runOnGLThread {
                //engine.glImageUnloaded(this)  // TODO
                if( engine.target == toDel)
                    engine.target = null
                engine.gl.deleteTexture(toDel)
            }
        }
    }

    override fun deepCopy(): RawImage = GLImage(this)

    override fun getRGB(x: Int, y: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    protected fun finalize() {
        flush()
    }
}