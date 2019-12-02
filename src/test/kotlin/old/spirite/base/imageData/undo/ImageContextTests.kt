package old.spirite.base.imageData.undo

import io.mockk.mockk
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.undo.ImageAction
import spirite.base.imageData.undo.ImageContext
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test as test

class ImageContextTests {
    val mockWorkspace = mockk<MImageWorkspace>(relaxed = true)
    val mediumHandle = MediumHandle(mockWorkspace, 9)
    val mockMediumRepo = mockk<MMediumRepository>(relaxed = true)
    val contextUnderTest = ImageContext(mediumHandle, mockWorkspace)

    @test fun doesUndoRedo3() {
        val action1 = TestImageAction(ArrangedMediumData(mediumHandle))
        val action2 = TestImageAction(ArrangedMediumData(mediumHandle))
        val action3 = TestImageAction(ArrangedMediumData(mediumHandle))
        contextUnderTest.addAction(action1)
        contextUnderTest.addAction(action2)
        contextUnderTest.addAction(action3)
        contextUnderTest.undo() // 1 2
        contextUnderTest.undo() // 1
        contextUnderTest.undo()
        assertEquals(2, action1.performCount)
        assertEquals(1, action2.performCount)
        assertEquals(0, action3.performCount)
        contextUnderTest.redo() // 1
        contextUnderTest.redo() // 2
        contextUnderTest.redo() // 3
        assertEquals(3, action1.performCount)
        assertEquals(2, action2.performCount)
        assertEquals(1, action3.performCount)
    }

    @test fun doesUndoRedo30() {
        val actions = List<ImageAction>(30, { TestImageAction(ArrangedMediumData(mediumHandle))})
        actions.forEach { contextUnderTest.addAction(it)}
        actions.forEach { contextUnderTest.undo() }
        actions.forEach { contextUnderTest.redo() }
        actions.forEach { contextUnderTest.undo() }
        actions.forEach { contextUnderTest.redo() }
    }

    @test fun clipsHead() {
        val actions = List<TestImageAction>(5, { TestImageAction(ArrangedMediumData(mediumHandle))})
        actions.forEach { contextUnderTest.addAction(it)}
        contextUnderTest.undo()
        contextUnderTest.undo()
        contextUnderTest.clipHead()

        assert(actions[3].dispatched)
        assert(actions[4].dispatched)
        assertEquals(3, contextUnderTest.effectivePointer)
        assertEquals(3, contextUnderTest.size)
    }

    @test fun clipsTail() {
        val actions = List<TestImageAction>(5, { TestImageAction(ArrangedMediumData(mediumHandle))})
        actions.forEach { contextUnderTest.addAction(it)}
        contextUnderTest.undo()
        contextUnderTest.undo()
        contextUnderTest.clipTail()


        assertEquals(2, contextUnderTest.effectivePointer)
        assertEquals(4, contextUnderTest.size)
    }
    @test fun clipsTail15() {
        val actions = List<TestImageAction>(15, { TestImageAction(ArrangedMediumData(mediumHandle))})
        actions.forEach { contextUnderTest.addAction(it)}
        contextUnderTest.undo()
        contextUnderTest.undo()
        (0..11).forEach { contextUnderTest.clipTail() }

        // First 10 actions will be dispatched, first one will be the initial Keyframe Action, so the first 9 inserted
        //  actions will be dispatched
        (0..8).forEach { assert( actions[it].dispatched) }
        (9..14).forEach { assert( !actions[it].dispatched) }

        assertEquals(1, contextUnderTest.effectivePointer)  // 15 - 12 dispatched - 2 undo
        assertEquals(3, contextUnderTest.size)              // 15 - 12 dispatched
    }



    class TestImageAction(arranged: ArrangedMediumData) : ImageAction(arranged) {
        var performCount = 0
        var dispatched = false
        override val description: String get() = "TUIA"

        override fun undoAction() {}

        override fun performImageAction(built: BuiltMediumData) {
            performCount++
        }

        override fun onDispatch() {
            dispatched = true
        }
    }
}