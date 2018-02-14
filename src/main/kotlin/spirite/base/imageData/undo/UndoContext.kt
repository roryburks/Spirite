package spirite.base.imageData.undo

import spirite.base.imageData.MediumHandle

/**
 * An UndoContext is at its heart simple a Storage Structure for UndoActions, however their iterative behavior can
 * differ wildly depending on implementation
 * */
interface UndoContext : Iterable<UndoableAction>{
    val medium: MediumHandle?
    fun flush()
    fun getImageDependencies() : Set<MediumHandle>

    /** Removes all UndoActions*/
    fun clipHead()
}

class NullContext : UndoContext {
    override fun clipHead() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val medium: MediumHandle? = null

    override fun getImageDependencies(): Set<MediumHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<UndoableAction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class CompositeContext : UndoContext {
    override fun clipHead() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val medium: MediumHandle? = null
    override fun iterator(): Iterator<UndoableAction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getImageDependencies(): Set<MediumHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class ImageContext(override val medium: MediumHandle) : UndoContext {
    override fun clipHead() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<UndoableAction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getImageDependencies(): Set<MediumHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}