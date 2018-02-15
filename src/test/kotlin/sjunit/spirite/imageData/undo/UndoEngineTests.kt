package sjunit.spirite.imageData.undo

import io.mockk.mockk
import sjunit.spirite.imageData.undo.ImageContextTests.TestImageAction
import sjunit.spirite.imageData.undo.NullContextTests.TestNullAction
import spirite.base.imageData.BuildingMediumData
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.undo.CompositeAction
import spirite.base.imageData.undo.UndoEngine
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test as test

class UndoEngineTests {
    private val _mockImageWorkspace = mockk<IImageWorkspace>(relaxed = true)
    private val engine = UndoEngine( _mockImageWorkspace)

    @test fun testPerforms() {
        val action1 = TestNullAction()
        engine.performAndStore( action1)
        assertEquals(1, action1.performCount)
        assertEquals(1, engine.queuePosition)
    }

    @test fun testPerforms3() {
        val action1 = TestNullAction()
        val action2 = TestNullAction()
        val action3 = TestNullAction()
        engine.performAndStore( action1)
        engine.performAndStore( action2)
        engine.performAndStore( action3)
        assertEquals(1, action1.performCount)
        assertEquals(1, action2.performCount)
        assertEquals(1, action3.performCount)
        assertEquals(3, engine.queuePosition)
    }
    @test fun testPerformsAndUndos3() {
        val action1 = TestNullAction()
        val action2 = TestNullAction()
        val action3 = TestNullAction()
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
        val action1 = TestNullAction()
        val action2 = TestNullAction()
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

    @test fun constructsProperHistory() {
        val actions = listOf(
                TestNullAction(),
                TestImageAction(BuildingMediumData( MediumHandle(_mockImageWorkspace, 1))),
                CompositeAction(List(2,{TestNullAction()}), "description"),
                TestImageAction(BuildingMediumData( MediumHandle(_mockImageWorkspace, 2))),
                TestNullAction())
        actions.forEach { engine.performAndStore(it) }

        val history = engine.undoHistory

        actions.zip(history, {action, history -> assertSame(action, history.action)})
    }
}