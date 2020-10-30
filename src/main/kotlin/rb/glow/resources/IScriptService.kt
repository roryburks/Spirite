package rb.glow.resources

/***
 * IScriptService mediates between some Script repository (be it hard-coded, file-based, or db-based) and the GLEngine's
 * request for scripts.  (particularly GLSL scripts, but any text file could be used)
 */
interface IScriptService {
    fun loadScript( scriptName: String) : String
}