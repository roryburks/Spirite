package rb.global

import rb.extendo.dataStructures.SinglyList

enum class SuccessKind {
    Success,
    Warning,
    Failure
}

class SuccessResponse(
    val errors: List<String>? = null,
    val warnings: List<String>? = null )
{
    fun kind() : SuccessKind {
        if( errors?.any() == true)
            return SuccessKind.Failure
        else if( warnings?.any() == true)
            return SuccessKind.Warning
        return SuccessKind.Success
    }

    companion object {
        fun Error(error: String) = SuccessResponse(errors = SinglyList(error))
        fun Warning(warning: String) = SuccessResponse(warnings = SinglyList(warning))
    }
}
