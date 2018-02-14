package sjunit.spirite.imageData.undo

import io.mockk.mockk
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.UndoEngine
import spirite.base.imageData.undo.UndoableAction
import org.junit.Test as test

class UndoEngineTests {
    val _mockImageWorkspace = mockk<IImageWorkspace>(relaxed = true)
    val engine = UndoEngine( _mockImageWorkspace)

    @test fun test() {
        val ta = TestUndoAction()
        engine.performAndStore( ta)
    }


    class TestUndoAction() : NullAction() {
        var performCount = 0
        var undoCount = 0

        override val description: String
            get() = "TUA"

        override fun performAction() {
            performCount++
        }

        override fun undoAction() {
            undoCount++
        }
    }
}