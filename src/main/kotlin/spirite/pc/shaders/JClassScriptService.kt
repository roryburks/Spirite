package spirite.pc.shaders

import rb.glow.resources.IScriptService
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug.ErrorType.RESOURCE
import java.io.IOException
import java.util.*

class JClassScriptService : IScriptService {
    override fun loadScript(scriptName: String): String {
        try {
            var ret = ""
            val resource = JClassScriptService::class.java.classLoader.getResource(scriptName)
            if( resource == null) {
                println("breakhere")
            }

            resource.openStream().use {
                val scanner = Scanner(it)
                scanner.useDelimiter("\\A")
                ret = scanner.next()
            }
            return ret
        }catch( e: IOException) {
            DebugProvider.debug.handleError(RESOURCE, "Couldn't load shader script file: [$scriptName]", e)
            return ""
        }
    }
}