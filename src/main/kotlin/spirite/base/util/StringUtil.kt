package spirite.base.util

// TODO: Determine where this really belongs and what it really should be called (maybe interfaced as a name-deduping service?)
object StringUtil {

    private val endNumRegex = """_([0-9]+)$""".toRegex()

    fun getNonDuplicateName( existingNames: Collection<String>, proposedName: String) : String {
        val existingTag = endNumRegex.find(proposedName)
        var i = existingTag?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val baseName = when( existingTag) {
            null -> proposedName
            else -> proposedName.substring(0,existingTag.range.start)
        }
        var nameToTry = proposedName

        while( existingNames.contains(nameToTry)) {
            nameToTry = "${baseName}_${++i}"
        }

        return nameToTry
    }
}