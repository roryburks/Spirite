package spirite.pc.resources

import spirite.base.resources.IScriptService
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.RESOURCE
import java.io.IOException
import java.util.*

class JClassScriptService : IScriptService {
    override fun loadScript(scriptName: String): String {
        try {
            var ret = ""
            JClassScriptService::class.java.classLoader.getResource(scriptName).openStream().use {
                val scanner = Scanner(it)
                scanner.useDelimiter("\\A")
                ret = scanner.next()
            }
            return ret
        }catch( e: IOException) {
            MDebug.handleError(RESOURCE, "Couldn't load shader script file: [$scriptName]", e)
            return ""
        }
    }
}