package spirite.hybrid

import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.IGLEngine
import rbJvm.glow.jogl.JOGLProvider
import spirite.base.graphics.gl.shader.GL330ShaderLoader
import spirite.pc.shaders.JClassScriptService

/** This serves little purpose other than being the single place that everything else gets its GLEngine singleton */
object EngineLaunchpoint {
    val gle : IGLEngine by lazy { GLEngine(
            { JOGLProvider.gl},
            GL330ShaderLoader(JOGLProvider.gl, JClassScriptService()))}
}