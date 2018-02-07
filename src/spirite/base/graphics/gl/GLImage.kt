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
        gl.bindTexture( GLC.TEXTURE_2D, tex)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)
        gl.texImage2D( GLC.TEXTURE_2D, 0, GLC.RGBA8, GLC.RGBA, GLC.UNSIGNED_BYTE,
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
        gl.bindTexture(GLC.TEXTURE_2D, tex)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)
        gl.copyTexImage2D(GLC.TEXTURE_2D, 0, GLC.RGBA8, 0, 0, width, height, 0)
    }

    constructor( tex: IGLTexture, width: Int, height: Int, glEngine: GLEngine, isGLOriented: Boolean) {
        this.tex = tex
        this.width = width
        this.height = height
        this.engine = glEngine
        this.isGLOriented = isGLOriented
    }
    // endregion

    override val graphics: GraphicsContext get() = GLGraphics(this)
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
        if (x < 0 || y < 0 || x >= width || y >= height) return 0
        val gl = engine.gl

        val read = gl.makeInt32Source(1)
        gl.readnPixels(x, if( isGLOriented) height-y-1 else y, 1, 1, GLC.BGRA, GLC.UNSIGNED_INT_8_8_8_8_REV, 4, read )
        return  read[0]
    }

    fun getRGBDirect( x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0
        val gl = engine.gl

        val read = gl.makeInt32Source(1)
        gl.readnPixels(x, y, 1, 1, GLC.BGRA, GLC.UNSIGNED_INT_8_8_8_8_REV, 4, read )
        return  read[0]
    }

    protected fun finalize() {
        flush()
    }
}