package sjunit.spirite.imageData.groupTree


import io.mockk.mockk
import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import kotlin.test.assertEquals
import org.junit.Test as test

open class PrimaryGroupTreeTests {
    val mockRenderEngine=  mockk<IRenderEngine>(relaxed = true)
    val mockSettingsManager = mockk<ISettingsManager>(relaxed = true)
    val mockPaletteManager = mockk<IPaletteManager>(relaxed = true)
    val workspace = ImageWorkspace(mockRenderEngine, mockSettingsManager, mockPaletteManager)
    val tree = workspace.groupTree

    @test fun CreatesSimpleLayer() {
        tree.addNewSimpleLayer(null, "Simple", FLAT, 30, 45)

        val ancestors = tree.root.getAllAncestors()
        val layerNode = ancestors.first() as LayerNode

        assertEquals(30, layerNode.layer.width)
        assertEquals(45, layerNode.layer.height)
        assert( (layerNode.layer as SimpleLayer).medium.medium is FlatMedium)
    }

    @test fun UndoesAndRedoes_AddNewSimpleLayer() {
        tree.addNewSimpleLayer(null, "Simple", FLAT, 30, 45)

        assertEquals(1, tree.root.getAllAncestors().count())

        workspace.undoEngine.undo()

        assertEquals(0, tree.root.getAllAncestors().count())
        workspace.undoEngine.redo()

        assertEquals(1, tree.root.getAllAncestors().count())

        val ancestors = tree.root.getAllAncestors()
        val layerNode = ancestors.first() as LayerNode
        assertEquals(30, layerNode.layer.width)
        assertEquals(45, layerNode.layer.height)
        assert( (layerNode.layer as SimpleLayer).medium.medium is FlatMedium)
    }

    @test fun UndoesAndRedoes_PropertyEdit() {
        tree.addNewSimpleLayer(null, "Simple", FLAT, 30, 45)

        val layerNode = tree.root.getLayerNodes().first()

        layerNode.x = 10
        layerNode.x = 20
        assertEquals(20, layerNode.x)

        workspace.undoEngine.undo()
        assertEquals(10, layerNode.x)
        workspace.undoEngine.undo()
        assertEquals(0, layerNode.x)

        workspace.undoEngine.redo()
        assertEquals(10, layerNode.x)
        workspace.undoEngine.redo()
        assertEquals(20, layerNode.x)
    }

    @test fun CreatesNestedGroups() {
        // Group 1
        // ---- Group 2
        // -------- Group 4
        // Group 3
        val group1 = tree.addGroupNode(null, "Group1")
        val group2 = tree.addGroupNode(group1, "Group2")
        val group3 = tree.addGroupNode(null, "Group3")
        val group4 = tree.addGroupNode(group2, "Group4")

        var ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)
        assert( group4.parent == group2)

        workspace.undoEngine.undo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)

        workspace.undoEngine.undo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( !ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)

        workspace.undoEngine.redo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( !ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)

        workspace.undoEngine.redo()
        ancestors = tree.root.getAllAncestors()
        assert( ancestors.contains( group1))
        assert( ancestors.contains( group2))
        assert( ancestors.contains( group3))
        assert( ancestors.contains( group4))

        assert( group1.parent == tree.root)
        assert( group2.parent == group1)
        assert( group3.parent == tree.root)
        assert( group4.parent == group2)
    }

}