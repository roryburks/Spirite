package rb.extendo.extensions

// An "Atlas" is just a cute way of talking about maps of maps (mutable on both tiers), which I have a reasonable amount
// of use cases for and specialty functions to.  It's possible this abstraction layer is too clever and too cutesy

typealias Atlas<T1,T2,T3> = MutableMap<T1,MutableMap<T2,T3>>

fun <T1,T2,T3> atlasOf() = mutableMapOf<T1,MutableMap<T2,T3>>()
