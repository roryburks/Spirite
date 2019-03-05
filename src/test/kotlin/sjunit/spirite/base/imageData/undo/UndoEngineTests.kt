//package sjunit.spirite.base.imageData.undo
//
//import io.mockk.mockk
//import sjunit.spirite.base.imageData.undo.ImageContextTests.TestImageAction
//import sjunit.spirite.base.imageData.undo.NullContextTests.TestNullAction
//import spirite.base.imageData.IImageWorkspace
//import spirite.base.imageData.MMediumRepository
//import spirite.base.imageData.MediumHandle
//import spirite.base.imageData.mediums.ArrangedMediumData
//import spirite.base.imageData.undo.*
//import kotlin.test.assertEquals
//import kotlin.test.assertSame
//import org.junit.Test as test
//
//class UndoEngineTests {
//    private val _mockImageWorkspace = mockk<IImageWorkspace>(relaxed = true)
//    val mockMediumRepo = mockk<MMediumRepository>(relaxed = true)
//    private val engine = UndoEngine( _mockImageWorkspace, mockMediumRepo)
//
//    @test fun testPerforms() {
//        val action1 = TestNullAction()
//        engine.performAndStore( action1)
//        assertEquals(1, action1.performCount)
//        assertEquals(1, engine.queuePosition)
//    }
//
//    @test fun testPerforms3() {
//        val action1 = TestNullAction()
//        val action2 = TestNullAction()
//        val action3 = TestNullAction()
//        engine.performAndStore( action1)
//        engine.performAndStore( action2)
//        engine.performAndStore( action3)
//        assertEquals(1, action1.performCount)
//        assertEquals(1, action2.performCount)
//        assertEquals(1, action3.performCount)
//        assertEquals(3, engine.queuePosition)
//    }
//    @test fun testPerformsAndUndos3() {
//        val action1 = TestNullAction()
//        val action2 = TestNullAction()
//        val action3 = TestNullAction()
//        engine.performAndStore( action1)
//        engine.performAndStore( action2)
//        engine.performAndStore( action3)
//        engine.undo()
//        engine.undo()
//        engine.undo()
//        engine.redo()
//        engine.redo()
//        engine.redo()
//        assertEquals(2, action1.performCount)
//        assertEquals(2, action2.performCount)
//        assertEquals(2, action3.performCount)
//        assertEquals(1, action1.undoCount)
//        assertEquals(1, action2.undoCount)
//        assertEquals(1, action3.undoCount)
//        assertEquals(1, action1.met)
//        assertEquals(1, action2.met)
//        assertEquals(1, action3.met)
//        assertEquals(3, engine.queuePosition)
//    }
//    @test fun handlesOverflowUndosAndRedos() {
//        val action1 = TestNullAction()
//        val action2 = TestNullAction()
//        engine.performAndStore( action1)
//        engine.performAndStore( action2)
//        engine.undo()
//        engine.undo()
//        engine.undo()   // false
//        engine.undo()   // false
//        engine.redo()
//        engine.redo()
//        engine.redo()   // false
//        engine.undo()
//        engine.redo()
//        engine.redo()   // false
//        assertEquals(2, action1.performCount)
//        assertEquals(3, action2.performCount)
//        assertEquals(1, action1.undoCount)
//        assertEquals(2, action2.undoCount)
//        assertEquals(1, action1.met)
//        assertEquals(1, action2.met)
//        assertEquals(2, engine.queuePosition)
//    }
//
//    @test fun clipsHeads() {
//        val action1 = TestNullAction()
//        val action2 = TestNullAction()
//        val action3 = TestNullAction()
//        engine.performAndStore( action1)
//        engine.performAndStore( action2)
//        engine.undo()
//        engine.performAndStore(action3)
//
//        val history = engine.undoHistory
//        assertEquals(2, history.size)
//        assert( history[0].action == action1)
//        assert( history[1].action == action3)
//    }
//
//    @test fun constructsProperHistory() {
//        val actions = listOf(
//                TestNullAction(),
//                TestImageAction(ArrangedMediumData(MediumHandle(_mockImageWorkspace, 1))),
//                CompositeAction(List(2,{TestNullAction()}), "description"),
//                TestImageAction(ArrangedMediumData(MediumHandle(_mockImageWorkspace, 2))),
//                TestNullAction())
//        actions.forEach { engine.performAndStore(it) }
//
//        val history = engine.undoHistory
//
//        actions.zip(history, {action, history -> assertSame(action, history.action)})
//    }
//
//    @test fun stacksActions() {
//        val action1 = TestNullAction()
//        val action2 = TestStackAction(0, 5)
//        val action3 = TestStackAction(0, 10)
//        val action4 = TestStackAction(1, 15)
//        val action5 = TestNullAction()
//        val action6 = TestStackAction(1, 20)
//
//        engine.performAndStore(action1)
//        engine.performAndStore(action2)
//        engine.performAndStore(action3)
//        engine.performAndStore(action4)
//        engine.performAndStore(action5)
//        engine.performAndStore(action6)
//
//        val history = engine.undoHistory
//
//        assert( history.count() == 5)
//        assert( history[0].action == action1)
//        assert( history[1].action == action2)
//        assert( history[2].action == action4)
//        assert( history[3].action == action5)
//        assert( history[4].action == action6)
//        assertEquals(10, action2.to)
//    }
//
//    class TestStackAction(val base: Int, var to: Int) : NullAction(), StackableAction {
//        var performCount = 0
//        var undoCount = 0
//        override val description: String get() = "Action"
//        override fun performAction() {performCount++}
//        override fun undoAction() {undoCount++}
//
//        override fun canStack(other: UndoableAction): Boolean {
//            return (other as? TestStackAction)?.base == base
//        }
//
//        override fun stackNewAction(other: UndoableAction) {
//            this.to = (other as TestStackAction).to
//        }
//    }
//
//    @test fun TestRecursiveAggregate() {
//        val action1 = TestNullAction()
//        val action2 = TestNullAction()
//        val action3 = TestNullAction()
//        engine.doAsAggregateAction("Outer"){
//            engine.doAsAggregateAction("Middle"){
//                engine.doAsAggregateAction("Inner"){
//                    engine.performAndStore(action1)
//                }
//                engine.performAndStore(action2)
//            }
//            engine.performAndStore(action3)
//        }
//
//        val history = engine.undoHistory
//        assertEquals(1, history.count())
//        val actions = (history[0].action as CompositeAction).actions
//        assertEquals(3, actions.count())
//        assertEquals(action1, actions[0])
//        assertEquals(action2, actions[1])
//        assertEquals(action3, actions[2])
//    }
//}