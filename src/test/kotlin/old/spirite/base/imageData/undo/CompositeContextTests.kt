package old.spirite.base.imageData.undo


import io.mockk.mockk
import old.spirite.base.imageData.undo.NullContextTests.TestNullAction
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.undo.CompositeAction
import spirite.base.imageData.undo.CompositeContext
import spirite.base.imageData.undo.ImageContext
import spirite.base.imageData.undo.NullContext
import kotlin.test.assertEquals
import org.junit.Test as test

class CompositeContextTests {
    val mockWorkspace = mockk<MImageWorkspace>(relaxed = true)
    val mockMediumRepo = mockk<MMediumRepository>(relaxed = true)
    val mediumHandle = MediumHandle(mockWorkspace, 9)
    val imageContexts = mutableListOf<ImageContext>()
    val nullContext = NullContext()
    val contextUnderTest = CompositeContext(nullContext, imageContexts,mockWorkspace)

    @test fun doesUndoRedo5() {
        val actions = List(5, { CompositeAction(List(2,{ TestNullAction() }), "action$it") })
        actions.forEach { contextUnderTest.addAction(it) }
        actions.forEach { contextUnderTest.undo() }
        actions.forEach { contextUnderTest.redo() }
        assertEquals(5, contextUnderTest.pointer)
        assertEquals(5, contextUnderTest.size)
    }

    @test fun clipsHead() {
        val actions = List(5, { CompositeAction(List(2,{ TestNullAction() }), "action$it") })
        actions.forEach { contextUnderTest.addAction(it) }
        contextUnderTest.undo()
        contextUnderTest.undo()
        contextUnderTest.clipHead()
        assertEquals(3, contextUnderTest.pointer)
        assertEquals(3, contextUnderTest.size)
    }

    @test fun clipsTail()
    {
        val actions = List(5, { CompositeAction(List(2,{ TestNullAction() }), "action$it") })
        actions.forEach { contextUnderTest.addAction(it) }
        contextUnderTest.undo()
        contextUnderTest.undo()
        contextUnderTest.clipTail()
        assertEquals(2, contextUnderTest.pointer)   // 5 start - 1 size - 2 undos
        assertEquals(4, contextUnderTest.size)      // 5 start - 1 size
    }
}