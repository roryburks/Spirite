package spirite.core.file

object SifConstants {
    val chunks = setOf("IMGD","GRPT","ANIM","ANSP","PLTT","TPLT", "VIEW")

    val header : ByteArray get() = byteArrayOf(0x53, 0x49, 0x46, 0x46) // "SIFF"
    const val latestVersion = 0x0001_0010

    // :::: GroupNode Type Identifiers for the SIFF GroupTree Section
    const val NODE_GROUP = 0x00
    const val NODE_SIMPLE_LAYER = 0x01
    const val NODE_SPRITE_LAYER = 0x02
    const val NODE_REFERENCE_LAYER = 0x03
    const val NODE_PUPPET_LAYER = 0x04

    // :::: MediumType
    const val MEDIUM_PLAIN = 0x00
    const val MEDIUM_DYNAMIC = 0x01
    const val MEDIUM_PRISMATIC = 0x02
    const val MEDIUM_MAGLEV = 0x03

    // :::: Maglev Thing Type
    const val MAGLEV_THING_STROKE = 0
    const val MAGLEV_THING_FILL = 1

    // :::: AnimationType
    const val ANIM_FFA = 0x01
    const val ANIM_RIG = 0x02

    // :::: AnimationSpaceType
    const val ANIMSPACE_FFA = 0x01

    // :::: FFALayerType
    const val FFALAYER_GROUPLINKED = 0x01
    const val FFALAYER_LEXICAL = 0x02
    const val FFALAYER_CASCADING = 0x03

    const val FfaFrameMarker_Frame = 1
    const val FfaFrameMarker_StartLocalLoop = 2
    const val FfaFrameMarker_EndLocalLoop = 4
    const val FfaFrameMarker_Gap = 3

    // GroupNode Attribute Masks
    const val VisibleMask = 0b1
    const val ExpandedMask = 0b10
    const val FlattenedMask = 0b100

    const val PenMode_Normal = 1
    const val PenMode_KeepAlpha = 2
    const val PenMode_Behind = 3
}