package spirite.base.graphics

interface IResourceUseTracker {
    val bytesUsed: Int
}

class ResourceUseTracker : IResourceUseTracker {
    override val bytesUsed: Int
        get() = TODO("not implemented")

}