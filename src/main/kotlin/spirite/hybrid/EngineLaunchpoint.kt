package spirite.hybrid

import spirite.base.graphics.gl.GLEngine
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService

/** This serves little purpose other than being the single place that everything else gets its GLEngine singleton */
object EngineLaunchpoint {
    val gle = GLEngine({JOGLProvider.gl}, JClassScriptService())
}