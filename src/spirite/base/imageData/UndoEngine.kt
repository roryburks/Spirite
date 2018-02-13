package spirite.base.imageData

interface IUndoEngine {
    fun reset()
    var queuePosition: Int
    val metronome: Int

    fun prepareContext( handle: MediumHandle)
    val dataUsed : List<MediumHandle>

}

class UndoEngine {

}