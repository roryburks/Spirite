package sjunit.spirite.base.imageData.undo


import io.mockk.mockk
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.NullContext
import rb.extendo.dataStructures.SinglyList
import kotlin.test.assertEquals
import org.junit.Test as test

class NullContextTests {
    val underTest = NullContext()
    val mockWorkspace = mockk<IImageWorkspace>(relaxed = true)

    @test fun doesUndoRedo3() {
        val action1 = TestNullAction()
        val action2 = TestNullAction()
        val action3 = TestNullAction()
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

    @test fun clipsHead() {
        val action1 = TestNullAction()
        val action2 = TestNullAction()
        val action3 = TestNullAction()
        underTest.addAction(action1)
        underTest.addAction(action2)
        underTest.addAction(action3)
        underTest.undo()
        underTest.undo()
        underTest.undo()
        underTest.redo()
        underTest.clipHead()

        assert(action2.dispatched)
        assert(action3.dispatched)
        assertEquals(1, action1.performCount)
        assertEquals(0, action2.performCount)
        assertEquals( 1, underTest.size)
        underTest.undo()
        underTest.redo()
        assertEquals(2, action1.performCount)
        assertEquals(2, action1.undoCount)
        assertEquals(1, underTest.pointer)
    }

    @test fun clipsTail() {
        val action1 = TestNullAction()
        val action2 = TestNullAction()
        val action3 = TestNullAction()
        underTest.addAction(action1)
        underTest.addAction(action2)
        underTest.addAction(action3)
        underTest.clipTail()
        underTest.undo()

        assert(action1.dispatched)
        assertEquals(2, underTest.size)
        assertEquals(1, underTest.pointer)

        underTest.clipTail()
        underTest.clipTail()
        assert(action2.dispatched)
        assert(action3.dispatched)
    }

    @test fun getsDependencies() {
        val mediumHandle1 = MediumHandle(mockWorkspace, 1)
        val mediumHandle2 = MediumHandle(mockWorkspace, 2)
        val action1 = TestNullAction(mediumHandle1)
        val action2 = TestNullAction(mediumHandle2)
        val action3 = TestNullAction(mediumHandle2)
        val action4 = TestNullAction()
        underTest.getImageDependencies()
        underTest.addAction(action1)
        underTest.addAction(action2)
        underTest.addAction(action3)
        underTest.addAction(action4)

        val dependencies = underTest.getImageDependencies()

        assertEquals(2, dependencies.size)
        assert( dependencies.contains(mediumHandle1))
        assert( dependencies.contains(mediumHandle2))
    }


    class TestNullAction(
            val mediumHandle: MediumHandle? = null
    ) : NullAction() {
        var performCount = 0
        var undoCount = 0
        var met = 0
        var dispatched = false

        override val description: String get() = "TUA"

        override fun performAction() {
            met++
            performCount++
        }

        override fun undoAction() {
            met--
            undoCount++
        }

        override fun onDispatch() {
            dispatched = true
        }

        override fun getDependencies(): Collection<MediumHandle>? {
            return if(mediumHandle == null) null else SinglyList(mediumHandle)
        }
    }
}