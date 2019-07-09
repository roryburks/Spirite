package rb.glow.gl

import rb.glow.RawImage
import rb.glow.RawImage.InvalidImageDimensionsExeption
import rb.glow.color.toColor
import rb.glow.color.toColorPremultiplied
import rb.glow.gle.GLGraphicsContext
import rb.glow.gle.GLParameters
import rb.glow.gle.IGLEngine

class GLImage : RawImage {
    override val width : Int
    override val height: Int
    val engine: IGLEngine
    val premultiplied: Boolean

    val tex: IGLTexture?
        get() {
            return if( flushed) null else _tex
        }
    internal val _tex : IGLTexture

    var flushed : Boolean = false

    // region Constructors
    constructor(width: Int, height: Int, glEngine: IGLEngine, premultiplied: Boolean = true) {
        if( width <= 0 || height <= 0)
            throw InvalidImageDimensionsExeption("Invalid Image Dimensions")
        this.width = width
        this.height = height
        this.engine = glEngine
        this.premultiplied = premultiplied

        val gl = glEngine.gl

        _tex = gl.createTexture() ?: throw GLResourcException("Failed to create Texture")
        gl.bindTexture( GLC.TEXTURE_2D, _tex)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)
        gl.texImage2D( GLC.TEXTURE_2D, 0, GLC.RGBA8, GLC.RGBA, GLC.UNSIGNED_BYTE,
                gl.createBlankTextureSource(width, height))

        // TODO: Add back
//        if( ! (gl as JOGL).gl.glIsTexture((_tex as JOGLTexture).texId))
//        {
//            MDebug.handleWarning(INITIALIZATION, "Failed to initialize GL Image")
//        }
        gl.tracker.markGlImageLoaded(this)
    }

    constructor( toCopy: GLImage) {
        width = toCopy.width
        height = toCopy.height
        engine = toCopy.engine
        premultiplied = toCopy.premultiplied

        val gl = engine.gl

        // Set the GL Target as the other image's texture and copy the data
        engine.target =  toCopy.tex
        _tex = gl.createTexture() ?: throw GLResourcException("Failed to create Texture")
        gl.bindTexture(GLC.TEXTURE_2D, _tex)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)
        gl.copyTexImage2D(GLC.TEXTURE_2D, 0, GLC.RGBA8, 0, 0, width, height, 0)
        gl.tracker.markGlImageLoaded(this)
    }

    constructor(tex: IGLTexture, width: Int, height: Int, glEngine: IGLEngine, premultiplied: Boolean = true) {
        this._tex = tex
        this.width = width
        this.height = height
        this.engine = glEngine
        this.premultiplied = premultiplied
        glEngine.gl.tracker.markGlImageLoaded(this)
    }

    // endregion

    override val graphics: GLGraphicsContext get() = GLGraphicsContext(this)
    override val byteSize: Int get() = width*height*4

    val glParams : GLParameters get() = GLParameters(width, height, premultiplied = premultiplied)

    override fun flush() {
        val gl = engine.gl

        if( !flushed) {
            flushed = true
            // Must be run on the AWT Thread to prevent JOGL-internal deadlocks
            engine.runOnGLThread {
                gl.tracker.markGLImageUnloaded(this)

                //engine.glImageUnloaded(this)  // TODO
                if( engine.target == _tex)
                    engine.target = null
                engine.gl.deleteTexture(_tex)
            }
        }
    }

    override fun deepCopy(): RawImage = GLImage(this)

    override fun getColor(x: Int, y: Int) =
            if( premultiplied) getARGB(x,y).toColorPremultiplied()
            else getARGB(x,y).toColor()

    override fun getARGB(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0
        engine.setTarget(this)
        val gl = engine.gl

        val read = gl.makeInt32Source(1)
        gl.readnPixels(x, y, 1, 1, GLC.BGRA, GLC.UNSIGNED_INT_8_8_8_8_REV, 4, read )
        return  read[0]
    }
}