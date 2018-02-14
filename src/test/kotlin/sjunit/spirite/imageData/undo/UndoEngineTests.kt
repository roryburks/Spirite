package sjunit.spirite.imageData.undo

import io.mockk.mockk
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.UndoEngine
import spirite.base.imageData.undo.UndoableAction
import kotlin.test.assertEquals
import org.junit.Test as test

class UndoEngineTests {
    val _mockImageWorkspace = mockk<IImageWorkspace>(relaxed = true)
    val engine = UndoEngine( _mockImageWorkspace)

    @test fun testPerforms() {
        val action1 = TestUndoAction()
        engine.performAndStore( action1)
        assertEquals(1, action1.performCount)
        assertEquals(1, engine.queuePosition)
    }

    @test fun testPerforms3() {
        val action1 = TestUndoAction()
        val action2 = TestUndoAction()
        val action3 = TestUndoAction()
        engine.performAndStore( action1)
        engine.performAndStore( action2)
        engine.performAndStore( action3)
        assertEquals(1, action1.performCount)
        assertEquals(1, action2.performCount)
        assertEquals(1, action3.performCount)
        assertEquals(3, engine.queuePosition)
    }
    @test fun testPerformsAndUndos3() {
        val action1 = TestUndoAction()
        val action2 = TestUndoAction()
        val action3 = TestUndoAction()
        engine.performAndStore( action1)
        engine.performAndStore( action2)
        engine.performAndStore( action3)
        engine.undo()
        engine.undo()
        engine.undo()
        engine.redo()
        engine.redo()
        engine.redo()
        assertEquals(2, action1.performCount)
        assertEquals(2, action2.performCount)
        assertEquals(2, action3.performCount)
        assertEquals(1, action1.undoCount)
        assertEquals(1, action2.undoCount)
        assertEquals(1, action3.undoCount)
        assertEquals(1, action1.met)
        assertEquals(1, action2.met)
        assertEquals(1, action3.met)
        assertEquals(3, engine.queuePosition)
    }
    @test fun handlesOverflowUndosAndRedos() {
        val action1 = TestUndoAction()
        val action2 = TestUndoAction()
        engine.performAndStore( action1)
        engine.performAndStore( action2)
        engine.undo()
        engine.undo()
        engine.undo()   // false
        engine.undo()   // false
        engine.redo()
        engine.redo()
        engine.redo()   // false
        engine.undo()
        engine.redo()
        engine.redo()   // false
        assertEquals(2, action1.performCount)
        assertEquals(3, action2.performCount)
        assertEquals(1, action1.undoCount)
        assertEquals(2, action2.undoCount)
        assertEquals(1, action1.met)
        assertEquals(1, action2.met)
        assertEquals(2, engine.queuePosition)
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