package sjunit.spirite.imageData.undo


import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.NullContext
import org.junit.Test as test

class NullContextTests {
    val underTest = NullContext()

    @test fun doesUndoRedo3() {
        val action1 = TestUndoAction()
        val action2 = TestUndoAction()
        val action3 = TestUndoAction()
        underTest.addAction(action1)
        underTest.addAction(action2)
        underTest.addAction(action3)
        underTest.undo()
        assert(action3.undoCount == 1)
        underTest.undo()
        assert(action2.undoCount == 1)
        underTest.undo()
        assert(action1.undoCount == 1)
        underTest.redo()
        assert(action1.performCount == 1)
        underTest.redo()
        assert(action2.performCount == 1)
        underTest.redo()
        assert(action3.performCount == 1)
    }

    class TestUndoAction() : NullAction() {
        var performCount = 0
        var undoCount = 0
        var met = 0

        override val description: String get() = "TUA"

        override fun performAction() {
            met++
            performCount++
        }

        override fun undoAction() {
            met--
            undoCount++
        }
    }
}