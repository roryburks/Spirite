package sguiSwing.hybrid

import rb.glow.gle.GLEngine
import rb.glow.gle.IGLEngine
import rbJvm.glow.awt.AwtImageConverter
import rbJvm.glow.gl.JvmGLImageConverter
import rbJvm.glow.jogl.GluPolygonTesselater
import rbJvm.glow.jogl.JOGLContext
import rbJvm.glow.jogl.JOGLProvider
import spirite.pc.shaders.JClassScriptService
import spirite.specialRendering.GL330ShaderLoader

/** This serves little purpose other than being the single place that everything else gets its GLEngine singleton */
object EngineLaunchpoint {
    val gle : IGLEngine by lazy {
        GLEngine(
                GluPolygonTesselater,
                GL330ShaderLoader(JOGLProvider.gl, JClassScriptService()),
                JvmGLImageConverter,
                JOGLContext())
    }

    val converter = AwtImageConverter {gle}
}