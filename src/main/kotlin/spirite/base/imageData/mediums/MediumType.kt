package spirite.base.imageData.mediums

enum class MediumType constructor(
        // This way, these values can be used in saving and loading without failing when
        //	an Enum is removed
        val permanentCode: Int,
        // Whether or not the user can directly create them (if they'll show up on the "Create Simple Layer" screen)
        val userCreatable: Boolean = true)
{
    FLAT(0),
    DYNAMIC(1),
    PRISMATIC(2),
    MAGLEV(3),
    DERIVED_MAGLEV(4, false);

    companion object {
        fun fromCode(code: Int): MediumType? {
            val values = MediumType.values()

            return values.indices
                    .firstOrNull { values[it].permanentCode == code }
                    ?.let { values[it] }
        }

        val creatableTypes: Array<MediumType> by lazy {
            MediumType.values().asList().filter { it.userCreatable }.toTypedArray()
        }
    }
}