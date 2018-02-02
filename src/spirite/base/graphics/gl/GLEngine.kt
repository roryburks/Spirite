package spirite.base.graphics.gl

import spirite.base.util.glu.GLC

class GLEngine(
        internal val gl: IGL) {


    val dbo : IGLRenderbuffer by lazy { gl.genRenderbuffer() }
    lateinit private var fbo : IGLFramebuffer

    var width : Int = 1 ; private set
    var height : Int = 1 ; private set

    var target: IGLTexture? = null
        get
        set(value) {
            if( field != value) {
                // Delete old Framebuffer
                if( field != null)
                    gl.deleteFramebuffer(fbo)

                if( value == null) {
                    gl.bindFrameBuffer(GLC.GL_FRAMEBUFFER, null)
                    field = value
                    width = 1
                    height = 1
                }
                else {
                    fbo = gl.genFramebuffer()
                    gl.bindFrameBuffer(GLC.GL_FRAMEBUFFER, fbo)

                    field = value
                    gl.bindRenderbuffer(GLC.GL_RENDERBUFFER, dbo)
                    gl.renderbufferStorage(GLC.GL_RENDERBUFFER, GLC.GL_DEPTH_COMPONENT16, 1, 1)
                    gl.framebufferRenderbuffer(GLC.GL_FRAMEBUFFER, GLC.GL_DEPTH_ATTACHMENT, GLC.GL_RENDERBUFFER, dbo)

                    // Attach Texture to FBO
                    gl.framebufferTexture2D( GLC.GL_FRAMEBUFFER, GLC.GL_COLOR_ATTACHMENT0, GLC.GL_TEXTURE_2D, value, 0)

                }
            }
        }

    fun setTarget(img: GLImage?) {
        if (img == null) {
            target = null
        } else {
            target = img.tex
            gl.viewport(0, 0, img.width, img.height)
            width = img.width
            height = img.height
        }
    }

    fun runOnGLThread( run: () -> Unit) {

    }
}