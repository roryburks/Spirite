package spirite.base.imageData

interface IUndoEngine {
    fun reset()
    var queuePosition: Int
    val metronome: Int

    fun prepareContext( handle: MediumHandle)
    val dataUsed : List<MediumHandle>

}

class UndoEngine : IUndoEngine{
    override fun reset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var queuePosition: Int
        get() = TODO("not implemented")
        set(value) {}
    override val metronome: Int
        get() = TODO("not implemented")

    override fun prepareContext(handle: MediumHandle) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val dataUsed: List<MediumHandle>
        get() = TODO("not implemented")

}