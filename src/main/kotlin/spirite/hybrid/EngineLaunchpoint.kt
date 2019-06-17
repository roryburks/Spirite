package spirite.hybrid

import rbJvm.glow.jogl.GluPolygonTesselater
import rb.glow.gle.GLEngine
import rb.glow.gle.IGLEngine
import rbJvm.glow.jogl.JOGLProvider
import spirite.specialRendering.GL330ShaderLoader
import spirite.pc.shaders.JClassScriptService

/** This serves little purpose other than being the single place that everything else gets its GLEngine singleton */
object EngineLaunchpoint {
    val gle : IGLEngine by lazy {
        GLEngine(
                { JOGLProvider.gl },
                GluPolygonTesselater,
                GL330ShaderLoader(JOGLProvider.gl, JClassScriptService()),
                AwtImageConverter())
    }
}